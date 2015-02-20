#!/usr/bin/env perl

use File::Find;

die "Specify 2 directories as arguments.\n" unless @ARGV==2;
map {
    die  $_." is not a directory.\n" unless -d $_ } @ARGV;

my $cwd = `pwd`;
chomp $cwd;

map {
    $_ = $cwd . '/'. $_ unless ($_ =~ /^\//);
    $_ =~ s/\/*$//; } @ARGV;

$local_dir = $ARGV[0];
$remote_dir = $ARGV[1];


find(\&diff,$ARGV[0]);

sub diff
{
 my $file = $_;
 my $local_file = $File::Find::name;
 (my $remote_file = $local_file) =~ s/$local_dir/$remote_dir/;

 next if -d $local_file;

 if (-e $remote_file) {
  print `diff -q $local_file $remote_file`;
 } else {
  print "$local_file does not have a mate.\n";
 }
}


