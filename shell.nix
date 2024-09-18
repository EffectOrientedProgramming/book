with import <nixpkgs> {};
mkShell {
  buildInputs = [
    pkgs.mkdocs
    pkgs.python311Packages.mkdocs-redirects
    pkgs.python311Packages.mkdocs-material
  ];
}
