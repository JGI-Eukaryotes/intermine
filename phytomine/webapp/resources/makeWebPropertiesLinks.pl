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

my @inList;

map { push(@inList, (split(/\|/,$_))[0] ) } @list;
map { s/\s+//g } @inList;

my $inList = '('.join(',',@inList).')';

print "Extracted ".scalar(@list)." proteome ids\n";

my $pac_out = `mysql -h dbcompgen PAC2_0 --skip-column-names -e "select id,taxId,jBrowsename,name from proteome where id in $inList"`;

open(FIL,">web.properties.links");

print FIL "## created by makeWebPropertiesLinks.pl at ".localtime(time)."\n";

my @lines = split(/\n/,$pac_out);

foreach my $line (@lines) {
  my ($id,$taxon,$jBName,$name) = split(/\s+/,$line,4);
  print FIL "attributelink.JBrowse.Gene.".$id.
            ".chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $jBName."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";
  print FIL "attributelink.JBrowse.Transcript.".$id.
            ".chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $jBName."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";
  print FIL "attributelink.JBrowse.Protein.".$id.
            ".transcripts.chromosomeLocation.paddedRegion.url=/jbrowse/index.html?data=genomes%2F".
            $jBName."&tracks=Transcripts%2CAlt_Transcripts%2CBlastx_protein&highlight=&loc=<<attributeValue>>\n";

  print FIL "attributelink.Phytozome.Gene.".$id.
            ".primaryIdentifier.url=/pz/portal.html#!gene?organism=".$jBName.
            "&searchText=locusName:<<attributeValue>>\n";
  print FIL "attributelink.Phytozome.Transcript.".$id.
            ".primaryIdentifier.url=/pz/portal.html#!gene?organism=".$jBName.
            "&searchText=transcriptName:<<attributeValue>>\n";
  print FIL "attributelink.Phytozome.Protein.".$id.
            ".transcript.primaryIdentifier.url=/pz/portal.html#!gene?organism=".$jBName.
            "&searchText=peptideName:<<attributeValue>>\n";

  print "organism $name\n";
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



