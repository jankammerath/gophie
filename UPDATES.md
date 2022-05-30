

# Updates


### Command line argument

If the URL of a Gopher directory (type 1) is found as command line
parameter, the address is opened.  The item must be a Gopher
directory.


### Multiple windows

Text documents, pictures ans searches are opened from a Gopher
directory the text is opened in a new Window.  Directories are opened
in the same.  This behaviour can be toggled by pressing the CTRL-key
when pressing the mouse button.


### HTTP support

Gophie can now fetch files from (some) HTTP servers (no HTTPS yet) by
implementing minimal support for HTTP/1.0 (request and Host-header).
URLs have HTTP semantics (i.e. no Gopher type character).

The content-type is ignored and the file's type is guessed from its
signature (magic bytes) just as Gophie is doing it.  The file
extension `.ca` is hard-wired to application/gopher-menu.  Try

  <http://www.quietsche-entchen.de/gopher/misc/gopher.ca>

(The space is not up-to-date but sufficient to illustrate the idea.)


### Directory parser

Gopher+ directory listings are recognized but only the data in the
+INFO: line (i.e. the standard Gopher information) is read.  If
directory entries are without server and port information that data is
filled with the respective data from the directory.  For HTTP, relative
selectors are supported.

Text documents that end with _index_, _home_, _contents_ or _help_ are
labeled as such.


### Links within documents

If inside a plain-text document text is selected the "Get and go"
option appears in the context menu (mouse right-click).  Selecting
this function interprets the highlighted text as URL and relative URLs
are supported.  A Gopher type identifier is not supported (in case the
document lives on a Gopher server).  The file's type is guess from
Java.

