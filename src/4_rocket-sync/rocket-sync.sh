#!/usr/bin/env bash

# Fail on unset variables and pipeline errors
set -o nounset
set -o pipefail

readonly VERSION="1.0.0"
readonly PROGNAME=$(basename "$0")

# Default configs (can be set in .rocket-sync file)
server="http://localhost:8080"
credentials="admin:admin"
force=false
quiet=false
composumUploadPath="/bin/cpm/package.service.html" # For 'put' (upload + install)
packageGroup="tmp/rocket-repo"
verbose=true
tmpDir=""

# --- Help Output ---

show_help() {
    cat <<EOF
NAME
    $PROGNAME - A tool for synchronizing JCR content with a Sling Rocket instance.

SYNOPSIS
    $PROGNAME put [OPTIONS] [<path>]

DESCRIPTION
    The '$PROGNAME' utility facilitates the transfer of FileVault-structured JCR content from a local file system to a Sling Rocket server running the Composum Package Manager.

    It operates on a specified path within a 'jcr_root' directory. For the given path, it dynamically creates a content package containing a single filter that encompasses the entire subtree of the path. This package is then uploaded and installed on the target Sling Rocket server.

    It is critical to note that this operation will overwrite any existing content on the server that falls within the scope of the filter.

COMMANDS
    put
        Uploads local file system content to the server.

ARGUMENTS
    <path>
        Specifies the local directory or file to be synchronized. If omitted, it defaults to the current working directory.

OPTIONS
    -h, --help
        Displays this help message and exits.

    -s <server>
        Specifies the target server URL, including the context path if necessary.
        Defaults to '$server'.

    -u <user>:<pwd>
        Specifies the credentials for server authentication.
        Defaults to '$credentials'.

    -f
        Executes the operation without interactive confirmation.

    -q
        Suppresses all output except for error messages.

CONFIGURATION
    The '$PROGNAME' utility can be configured via a '.rocket-sync' file. This file can be placed in the current directory or any of its parent directories. The utility searches for this file upwards from the current location.

    The configuration file can specify the 'server' and 'credentials'.

    Example '.rocket-sync' file:
        server=http://remote-server.com:9090
        credentials=deploy-user:secret-password

    Command-line options always take precedence over the settings in the configuration file.

EXAMPLES
    1. Upload changes to the server
       (assumes a Sling Rocket server is running on $server)

           cd jcr_root/apps/project
           # Modify a file
           vim .content.xml
           # Upload the entire directory
           $PROGNAME put
           # Add a new file
           touch file.jsp
           # Upload only the new file
           $PROGNAME put file.jsp

    2. Use a custom server and credentials

           $PROGNAME put -s localhost:8888 -u user:pwd

    3. Perform a non-interactive upload

           $PROGNAME put -f
EOF
}

show_hint_repo_config() {
    local repoCfg=${repoCfg:-"$rootpath"/.rocket-sync}
    cat <<EOF

Hint: You can set the server/credentials in a .rocket-sync config file:

EOF
    if [[ -n "${opt_server:-}" ]]; then
        echo "    echo server=$opt_server >> \"$repoCfg\""
    fi
    if [[ -n "${opt_credentials:-}" ]]; then
        echo "    echo credentials=user:*** >> \"$repoCfg\""
    fi
}

# --- General Script Utils ---

print() {
    if "$verbose"; then
        echo "$@"
    fi
}

echo_err() {
    printf "%s\n" "$*" >&2
}

cleanup() {
    if [[ -d "$tmpDir" ]]; then
        rm -rf "$tmpDir"
    fi
    exit "${1:-0}"
}

trap 'cleanup 130' SIGINT SIGTERM SIGHUP SIGQUIT

fail() {
    echo_err "$PROGNAME: $1"
    cleanup "${2:-1}"
}

userfail() {
    # use exit code 4 for user/argument errors
    fail "$1" 4
}

prompt() {
    read -p "$1 (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo_err "Aborted."
        cleanup 3
    fi
}

abspath() {
    # generate absolute path from relative path
    # $1: relative filename
    if [[ -d "$1" ]]; then
        (cd "$1" && pwd)
    elif [[ -f "$1" ]]; then
        if [[ "$1" == */* ]]; then
            echo "$(cd "${1%/*}" && pwd)/${1##*/}"
        else
            echo "$(pwd)/$1"
        fi
    fi
}

find_up() {
    # find a file upwards in the tree
    # $1: start directory
    # $2: file to find
    local dir
    dir=$(abspath "$1")
    while [[ -n "$dir" ]]; do
        if [[ -e "$dir/$2" ]]; then
            echo "$dir/$2"
            return
        fi
        dir=${dir%/*}
    done
}

# --- Package Creation Utils ---

urldecode() {
    printf '%b' "${1//%/\\x}"
}

filesystem_to_jcr() {
    # convert filesystem to JCR paths to be used as filters
    local filter="$1"
    filter=${filter%%/.content.xml}
    filter=${filter%%.xml}
    # rename known namespaces prefixes
    filter=${filter//_jcr_/jcr:}
    filter=${filter//_mix_/mix:}
    filter=${filter//_nt_/nt:}
    filter=${filter//_oak_/oak:}
    filter=${filter//_rep_/rep:}
    filter=${filter//_rocket_/rocket:}
    filter=${filter//_sling_/sling:}
    filter=${filter//_slingevent_/slingevent:}
    filter=${filter//_sv_/sv:}
    filter=${filter//_vlt_/vlt:}
    filter=${filter//_xml_/xml:}

    urldecode "$filter"
}

to_xml() {
    local name="$1"
    name=${name//&/&amp;}
    name=${name//</&lt;}
    name=${name//>/&gt;}
    name=${name//\"/&quot;}
    name=${name//\'/&apos;}
    echo "$name"
}

create_pkg_meta_inf() {
    local base="$1"
    local filter
    filter=$(filesystem_to_jcr "$2")
    local pkgGroup="$3"
    local pkgName="$4"
    local pkgVersion="$5"

    mkdir -p "$base/META-INF/vault"
    mkdir -p "$base/jcr_root"

    cat > "$base/META-INF/vault/filter.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<workspaceFilter version="1.0">
    <filter root="$(to_xml "$filter")"/>
</workspaceFilter>
EOF

    cat > "$base/META-INF/vault/properties.xml" <<EOF
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
<entry key="name">$(to_xml "$pkgName")</entry>
<entry key="version">$(to_xml "$pkgVersion")</entry>
<entry key="group">$(to_xml "$pkgGroup")</entry>
</properties>
EOF
}

build_zip() {
    local dir="$1"
    # zip must be executed inside the dir for the right paths
    (cd "$dir" && zip -q -r pkg.zip .)
    echo "$dir/pkg.zip"
}

# --- Package HTTP (Composum) ---

upload_and_install_pkg() {
    local zipfile="$1"
    print "Uploading '$zipfile' to '$server$composumUploadPath'"
    local out
    if ! out=$(curl -u "$credentials" -f -s -S -F "file=@$zipfile" "$server$composumUploadPath"); then
        fail "Failed to upload package: $out" 5
    fi
    print "Upload successful (assumed installed by Composum)."
}

# --- Main ---

main() {
    if [[ $# -eq 0 ]]; then
        userfail "No command given. Use 'put'. See usage help with -h."
    fi

    if [[ "$1" == "-h" || "$1" == "--help" || "$1" == "help" ]]; then
        show_help | less -FX
        exit
    fi

    if [[ "$1" == "-version" || "$1" == "--version" ]]; then
        echo "$PROGNAME version $VERSION"
        exit
    fi

    if [[ "$1" != "put" ]]; then
        userfail "Unknown command '$1'. This script only supports 'put'. See usage help with -h."
    fi

    shift # remove 'put' command

    # Parse arguments
    local path="$PWD"
    local opt_server=""
    local opt_credentials=""
    local opt_force=false
    local opt_quiet=false

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --help) show_help | less -FX; exit ;;
            -h) show_help | less -FX; exit ;;
            -s) opt_server="$2"; shift ;;
            -u) opt_credentials="$2"; shift ;;
            -f) opt_force=true ;;
            -q) opt_quiet=true ;;
            -*) userfail "Unrecognized option '$1'. See usage help with -h." ;;
            *) path="$1" ;;
        esac
        shift
    done

    if ! "$opt_quiet"; then
        verbose=true
    else
        verbose=false
    fi

    if [[ ! -e "$path" ]]; then
        userfail "Path does not exist: $path"
    fi

    path=$(abspath "$path")
    if [[ "$path" != */jcr_root* ]]; then
        userfail "Not inside a FileVault checkout with a jcr_root base directory: $path"
    fi

    local filter="${path##*/jcr_root}"
    local rootpath="${path%/jcr_root*}/jcr_root"
    filter=${filter:-/}

    if [[ "$filter" == "/" ]]; then
        userfail "Refusing to work on repository root (would be too slow or overwrite everything)."
    fi

    # Read config file
    local repoCfg
    repoCfg=$(find_up "$PWD" .rocket-sync)
    if [[ -f "$repoCfg" ]]; then
        source "$repoCfg"
    fi

    # Command line options take precedence
    server=${opt_server:-$server}
    credentials=${opt_credentials:-$credentials}
    force=${opt_force:-$force}
    quiet=${opt_quiet:-$quiet}

    local humanFilter
    if [[ -d "$path" ]]; then
        humanFilter="$filter/*"
    else
        humanFilter="$filter"
    fi

    print "Uploading $humanFilter to $server"

    local packageName
    packageName="rocket-repo-${filter//\//-}"
    packageName="${packageName//[^[:alnum:].-]/}"
    local packageVersion
    packageVersion=$(date +"%s")

    tmpDir=$(mktemp -d -t repo.XXX)

    create_pkg_meta_inf "$tmpDir" "$filter" "$packageGroup" "$packageName" "$packageVersion"

    local filterDirname="${filter%/*}"
    mkdir -p "$tmpDir/jcr_root$filterDirname"
    # Use rsync to copy content, excluding common unwanted files
    rsync -avq --exclude=".rocket-sync" --exclude=".DS_Store" "$path" "$tmpDir/jcr_root$filterDirname/"

    local zipfile
    zipfile=$(build_zip "$tmpDir")

    if "$verbose"; then
        unzip -l "$zipfile" | grep "   jcr_root$filter" || true
    fi

    if ! "$force"; then
        prompt "Upload and overwrite on server?"
    fi

    upload_and_install_pkg "$zipfile"

    if "$verbose"; then
        if [[ -n "$opt_server" || -n "$opt_credentials" ]]; then
            show_hint_repo_config
        fi
    fi

    cleanup
}

main "$@"
