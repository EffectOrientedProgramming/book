{ pkgs ? import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/06278c77b5d162e62df170fec307e83f1812d94b.tar.gz") {} }:

pkgs.mkShell {
  buildInputs = [
    pkgs.gitMinimal
    pkgs.scalafmt
    pkgs.scala
    pkgs.sbt
    pkgs.jetbrains.idea-community
    pkgs.metals
#    pkgs.vscode
    # Random others that might be helpful
    pkgs.vim
    pkgs.which
    pkgs.htop
    pkgs.zlib
  ];
}
