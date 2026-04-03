// SPDX-License-Identifier: MIT
/*
 * shell.nix
 *
 * Copyright (C) 2026           sidharthify <wednisegit@gmail.com>
 */

{ pkgs ? import <nixpkgs> {} }:
pkgs.mkShell {
  packages = [
    pkgs.jdk17
  ];
}