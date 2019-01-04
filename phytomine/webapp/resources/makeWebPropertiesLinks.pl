#!/usr/bin/env perl

use strict;

die("Usage: $0 <host> <mine>\n") unless @ARGV==2;

my $minehost = shift @ARGV;
my $mine = shift @ARGV;

# a list of organism full names
my %friendlyMines = ( );
#                      'Arabidopsis thaliana columbia' => [
#                                                {label=>'Araport',
#                                                 url=>'https://apps.araport.org/thalemine/'} , ],
#                      'Phaseolus vulgaris' => [
#                                                {label=>'BeanMine',
#                                                 url=>'https://mines.legumeinfo.org/beanmine/'}, 
#                      'Glycine max' => [
#                                                {label=>'SoyMine',
#                                                 url=>'https://mines.legumeinfo.org/soymine/'}, ],
#                      'Medicago truncatula' => [
#                                                {label=>'MedicMine',
#                                                 url=>'https://medicmine.jcvi.org/medicmine/'}, ],
#                      'Vigna unguiculata' => [
#                                                {label=>'ChickpeaMine',
#                                                 url=>'https://mines.legumeinfo.org/chickpeamine/'}, ],
#                      'Vitus vinifera' => [
#                                                {label=>'GrapeMine',
#                                                 url=>'http://urgi.versailles.inra.fa/GrapeMine/'}, ],
#                      'Cicer arietinum' => [
#                                                {label=>'CowpeaMine',
#                                                 url=>'https://mines.legumeinfo.org/cowpeamine/'},
#                                                 ]);



my $list = `psql -h $minehost -t $mine -c "select proteomeId,taxonId,name from organism"`;

my @list = split(/\n/,$list);

my %inList;

sub trim { my $a = shift; $a =~ s/^\s*//; $a =~ s/\s*$//; return $a }

map { my @f = split(/\|/,$_);
      $inList{trim($f[0])} = { 'taxon' => trim($f[1]), 'name' => trim($f[2])}; } @list;

print "Extracted ".scalar(@list)." proteome ids\n";

my $pac_out = `mysql -h dbcompgen deploy_config_metadata --skip-column-names -e "select proteome_id,m1.value from deploy_release, release_metadata m1  where deploy_id=5 and release_id=id and type_id=1"`;

open(FIL,">web.properties.links");

print FIL "## created by makeWebPropertiesLinks.pl at ".localtime(time)."\n";

my @lines = split(/\n/,$pac_out);

foreach my $line (@lines) {
  my ($id,$jBName) = split(/\s+/,$line,2);
  die "Proteome id $id not in the mine.\n" unless exists($inList{$id});
  my $taxon = $inList{$id}->{taxon};
  my $name = $inList{$id}->{name};
  print FIL "attributelink.JBrowse.Gene.".$id.
            ".chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $jBName."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";
  print FIL "attributelink.JBrowse.Transcript.".$id.
            ".chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $jBName."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";
  print FIL "attributelink.JBrowse.Protein.".$id.
            ".transcript.chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $jBName."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";

  print FIL "attributelink.Phytozome.Gene.".$id.
            ".primaryIdentifier.url=/report/gene/".$jBName.
            "/<<attributeValue>>\n";
  print FIL "attributelink.Phytozome.Transcript.".$id.
            ".primaryIdentifier.url=/report/transcript/".$jBName.
            "/<<attributeValue>>\n";
  print FIL "attributelink.Phytozome.Protein.".$id.
            ".primaryIdentifier.url=/report/protein/".$jBName.
            "/<<attributeValue>>\n";

  print "proteome $id $jBName\n";
  if (exists($friendlyMines{$name}) ) {
    my $mine = $friendlyMines{$name}->{label};
    my $url = $friendlyMines{$name}->{url};
    print FIL "attributelink.$mine.Gene.$taxon.primaryIdentifier.list.text=$mine List Report\n";
    print FIL "attributelink.$mine.Gene.$taxon.primaryIdentifier.list.url=$url/portal.do?class=Gene&externalids=<<attributeValue>>\n";
    print FIL "attributelink.$mine.Gene.$taxon.primaryIdentifier.list.usePost=true\n";
    print FIL "attributelink.$mine.Transcript.$taxon.primaryIdentifier.list.text=$mine List Report\n";
    print FIL "attributelink.$mine.Transcript.$taxon.primaryIdentifier.list.url=$url/portal.do?class=Transcript&externalids=<<attributeValue>>\n";
    print FIL "attributelink.$mine.Transcript.$taxon.primaryIdentifier.list.usePost=true\n";
    print FIL "attributelink.$mine.Protein.$taxon.primaryIdentifier.list.text=$mine List Report\n";
    print FIL "attributelink.$mine.Protein.$taxon.primaryIdentifier.list.url=$url/portal.do?class=Protein&externalids=<<attributeValue>>\n";
    print FIL "attributelink.$mine.Protein.$taxon.primaryIdentifier.list.usePost=true\n";

    print FIL "attributelink.$mine.Gene.$id.primaryIdentifier.text=$mine <<attributeValue>> Report\n";
    print FIL "attributelink.$mine.Gene.$id.primaryIdentifier.url=$url/portal.do?class=Gene&externalids=<<attributeValue>>\n";
    print FIL "attributelink.$mine.Transcript.$id.primaryIdentifier.text=$mine <<attributeValue>> Report\n";
    print FIL "attributelink.$mine.Transcript.$id.primaryIdentifier.url=$url/portal.do?class=Transcript&externalids=<<attributeValue>>\n";
    print FIL "attributelink.$mine.Protein.$id.primaryIdentifier.text=$mine <<attributeValue>> Report\n";
    print FIL "attributelink.$mine.Protein.$id.primaryIdentifier.url=$url/portal.do?class=Protein&externalids=<<attributeValue>>\n";
  }
}

print FIL "## end of section created by makeWebPropertiesLinks.pl\n";

close(FIL);

