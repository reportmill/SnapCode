#!/usr/bin/env bash
#
# Build this checkout of SnapCode for the browser and serve it locally, so the WebMCP tools
# (src/snapcode/webmcp, see WebMCP.md) can be used by an AI agent in a browser.
#
#   scripts/run-webmcp-local.sh [PORT]
#
# Needs git, curl, python3 and a JDK 21 or newer (javac + jar) that still includes the
# jdk.jsobject module, which the browser glue compiles against (deprecated for removal in newer JDKs).
#
# SnapKit, SnapBuilder and Greenfoot are used from sibling directories (the layout build.gradle
# expects) and cloned there if missing. Build output goes to build/webmcp-local/.
# Then open http://localhost:PORT/index.html?loader=none
set -euo pipefail

PORT="${1:-8765}"
SNAPCODE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PARENT_DIR="$(dirname "$SNAPCODE_DIR")"
OUT_DIR="$SNAPCODE_DIR/build/webmcp-local"

# 0. The browser glue needs the jdk.jsobject module.
if ! java --list-modules 2>/dev/null | grep -q '^jdk.jsobject'; then
  echo "This build needs a JDK 21+ that includes the jdk.jsobject module; 'java' is: $(java -version 2>&1 | head -1)" >&2
  if [ -x /usr/libexec/java_home ]; then
    echo "On macOS:  export JAVA_HOME=\$(/usr/libexec/java_home -v 21) PATH=\$JAVA_HOME/bin:\$PATH" >&2
  fi
  exit 1
fi

# 1. Libraries SnapCode builds against, as siblings of this checkout.
for repo in SnapKit SnapBuilder Greenfoot; do
  if [ ! -d "$PARENT_DIR/$repo" ]; then
    echo "Cloning $repo next to SnapCode..."
    git clone --depth 1 "https://github.com/reportmill/$repo" "$PARENT_DIR/$repo"
  fi
done

# 2. Compile-time jars (JGit, used by SnapCode's desktop Git support).
mkdir -p "$OUT_DIR/libs"
for u in org/eclipse/jgit/org.eclipse.jgit/7.3.0.202506031305-r/org.eclipse.jgit-7.3.0.202506031305-r.jar \
         org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar \
         com/googlecode/javaewah/JavaEWAH/1.2.3/JavaEWAH-1.2.3.jar \
         commons-codec/commons-codec/1.17.1/commons-codec-1.17.1.jar; do
  f="$OUT_DIR/libs/$(basename "$u")"
  [ -f "$f" ] || curl -sSfL -o "$f" "https://repo1.maven.org/maven2/$u"
done

# 3. Compile everything and copy resources (.snp layouts, images, markdown, ...).
SRC_DIRS=("$PARENT_DIR/SnapKit/src" "$PARENT_DIR/SnapBuilder/src" "$PARENT_DIR/Greenfoot/src" "$SNAPCODE_DIR/src")
CLASSES="$OUT_DIR/classes"
rm -rf "$CLASSES" && mkdir -p "$CLASSES"
find "${SRC_DIRS[@]}" -name '*.java' > "$OUT_DIR/sources.txt"
echo "Compiling $(wc -l < "$OUT_DIR/sources.txt" | tr -d ' ') files..."
javac -nowarn -Xlint:-removal -proc:none -d "$CLASSES" -cp "$OUT_DIR/libs/*" --add-modules jdk.jsobject @"$OUT_DIR/sources.txt"
for d in "${SRC_DIRS[@]}"; do
  # tar keeps the directory structure; works with both GNU (Linux) and BSD (macOS) tools.
  (cd "$d" && find . -type f ! -name '*.java' | tar -cf - -T -) | (cd "$CLASSES" && tar -xf -)
done

# 4. The web app: one jar per project, plus the hosted page and loader.
WEB="$OUT_DIR/web"
rm -rf "$WEB" && mkdir -p "$WEB"
(cd "$CLASSES" && jar cf "$WEB/SnapKit.jar" snap \
               && jar cf "$WEB/SnapCode.jar" snapcode javakit \
               && jar cf "$WEB/SnapBuilder.jar" snapbuild \
               && jar cf "$WEB/Greenfoot.jar" greenfoot)
cp "$PARENT_DIR/SnapKit/cjdom.js" "$WEB/"
for f in index.html launcher.html icon.png manifest.json; do
  curl -sSfL -o "$WEB/$f" "https://reportmill.com/SnapCode/app/$f"
done
# The hosted page names versioned jars (e.g. SnapKit-2026.10.jar); point it at the ones built here.
sed -i.bak -E 's/(SnapKit|SnapCode|SnapBuilder|Greenfoot)-[0-9][0-9.]*\.jar/\1.jar/g' "$WEB/index.html" && rm -f "$WEB/index.html.bak"

# 5. Serve it. CheerpJ needs HTTP Range requests, which python's plain http.server lacks.
cat > "$OUT_DIR/rangeserver.py" <<'PY'
import http.server, os, re, sys
class H(http.server.SimpleHTTPRequestHandler):
    def send_head(self):
        path = self.translate_path(self.path); rng = self.headers.get('Range')
        if not rng or os.path.isdir(path) or not os.path.exists(path):
            return super().send_head()
        m = re.match(r'bytes=(\d*)-(\d*)', rng); size = os.path.getsize(path)
        start = int(m.group(1)) if m.group(1) else max(0, size - int(m.group(2)))
        end = min(int(m.group(2)) if m.group(1) and m.group(2) else size - 1, size - 1)
        f = open(path, 'rb'); f.seek(start)
        self.send_response(206)
        self.send_header('Content-Type', self.guess_type(path))
        self.send_header('Content-Range', f'bytes {start}-{end}/{size}')
        self.send_header('Content-Length', str(end - start + 1))
        self.end_headers(); self._remaining = end - start + 1
        return f
    def copyfile(self, src, dst):
        n = getattr(self, '_remaining', None)
        if n is None: return super().copyfile(src, dst)
        while n > 0:
            b = src.read(min(65536, n))
            if not b: break
            dst.write(b); n -= len(b)
        self._remaining = None
    def end_headers(self):
        self.send_header('Accept-Ranges', 'bytes'); super().end_headers()
os.chdir(sys.argv[1])
http.server.ThreadingHTTPServer(('0.0.0.0', int(sys.argv[2])), H).serve_forever()
PY
echo
echo "SnapCode with WebMCP: http://localhost:$PORT/index.html?loader=none   (Ctrl+C to stop)"
exec python3 "$OUT_DIR/rangeserver.py" "$WEB" "$PORT"
