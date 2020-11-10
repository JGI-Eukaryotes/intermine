                                                                                    value                                                                                    
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 <model name="genomic" package="org.intermine.model.bio">                                                                                                                   +
 <class name="Intron" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000188">                                                       +
         <collection name="transcripts" referenced-type="Transcript" reverse-reference="introns"/>                                                                          +
         <collection name="genes" referenced-type="Gene" reverse-reference="introns"/>                                                                                      +
 </class>                                                                                                                                                                   +
 <class name="Allele" extends="SequenceCollection" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0001023">                                                    +
         <reference name="gene" referenced-type="Gene" reverse-reference="alleles"/>                                                                                        +
 </class>                                                                                                                                                                   +
 <class name="SyntenicRegion" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0005858">                                               +
         <reference name="syntenyBlock" referenced-type="SyntenyBlock" reverse-reference="syntenicRegions"/>                                                                +
 </class>                                                                                                                                                                   +
 <class name="Coexpression" extends="java.lang.Object" is-interface="false">                                                                                                +
         <attribute name="experimentGroup" type="java.lang.String"/>                                                                                                        +
         <attribute name="correlation" type="java.lang.Float"/>                                                                                                             +
         <attribute name="significance" type="java.lang.Float"/>                                                                                                            +
         <reference name="coexpressedGene" referenced-type="Gene"/>                                                                                                         +
         <reference name="gene" referenced-type="Gene"/>                                                                                                                    +
 </class>                                                                                                                                                                   +
 <class name="EST" extends="Oligo" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000345">                                                                    +
         <collection name="overlappingESTSets" referenced-type="OverlappingESTSet" reverse-reference="ESTs"/>                                                               +
 </class>                                                                                                                                                                   +
 <class name="ProteinFamilyMember" is-interface="true">                                                                                                                     +
         <attribute name="count" type="java.lang.Integer"/>                                                                                                                 +
         <attribute name="membershipDetail" type="java.lang.String"/>                                                                                                       +
         <reference name="protein" referenced-type="Protein"/>                                                                                                              +
         <reference name="proteinFamily" referenced-type="ProteinFamily" reverse-reference="member"/>                                                                       +
         <reference name="organism" referenced-type="Organism"/>                                                                                                            +
 </class>                                                                                                                                                                   +
 <class name="TransposableElementInsertionSite" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000368"></class>                     +
 <class name="CRM" extends="RegulatoryRegion" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000727">                                                         +
         <collection name="TFBindingSites" referenced-type="TFBindingSite" reverse-reference="CRM"/>                                                                        +
 </class>                                                                                                                                                                   +
 <class name="GOEvidence" is-interface="true">                                                                                                                              +
         <attribute name="withText" type="java.lang.String"/>                                                                                                               +
         <reference name="code" referenced-type="GOEvidenceCode"/>                                                                                                          +
         <collection name="publications" referenced-type="Publication"/>                                                                                                    +
         <collection name="with" referenced-type="BioEntity"/>                                                                                                              +
 </class>                                                                                                                                                                   +
 <class name="GoldenPathFragment" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000468"></class>                                   +
 <class name="Strain" extends="BioEntity" is-interface="true">                                                                                                              +
         <attribute name="annotationVersion" type="java.lang.String"/>                                                                                                      +
         <attribute name="assemblyVersion" type="java.lang.String"/>                                                                                                        +
         <collection name="features" referenced-type="SequenceFeature" reverse-reference="strain"/>                                                                         +
 </class>                                                                                                                                                                   +
 <class name="IntergenicRegion" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000605">                                             +
         <collection name="adjacentGenes" referenced-type="Gene"/>                                                                                                          +
 </class>                                                                                                                                                                   +
 <class name="ProteinFamily" is-interface="true">                                                                                                                           +
         <attribute name="methodName" type="java.lang.String"/>                                                                                                             +
         <attribute name="clusterName" type="java.lang.String"/>                                                                                                            +
         <attribute name="methodId" type="java.lang.Integer"/>                                                                                                              +
         <attribute name="clusterId" type="java.lang.Integer"/>                                                                                                             +
         <attribute name="memberCount" type="java.lang.Integer"/>                                                                                                           +
         <reference name="consensus" referenced-type="Sequence"/>                                                                                                           +
         <reference name="msa" referenced-type="MSA"/>                                                                                                                      +
         <collection name="genes" referenced-type="Gene" reverse-reference="proteinFamily"/>                                                                                +
         <collection name="crossReferences" referenced-type="CrossReference"/>                                                                                              +
         <collection name="member" referenced-type="ProteinFamilyMember" reverse-reference="proteinFamily"/>                                                                +
 </class>                                                                                                                                                                   +
 <class name="TransposableElement" extends="MobileGeneticElement" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000101"></class>                             +
 <class name="SnoRNA" extends="NcRNA" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000275"></class>                                                         +
 <class name="Annotatable" is-interface="true">                                                                                                                             +
         <attribute name="primaryIdentifier" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000675"/>                                                +
         <collection name="ontologyAnnotations" referenced-type="OntologyAnnotation" reverse-reference="subject"/>                                                          +
         <collection name="publications" referenced-type="Publication" reverse-reference="entities"/>                                                                       +
 </class>                                                                                                                                                                   +
 <class name="Primer" extends="Oligo" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000112"></class>                                                         +
 <class name="GOAnnotation" extends="OntologyAnnotation" is-interface="true">                                                                                               +
         <attribute name="annotationExtension" type="java.lang.String"/>                                                                                                    +
 </class>                                                                                                                                                                   +
 <class name="MobileGeneticElement" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0001037"></class>                                 +
 <class name="PathwayInfo" is-interface="true">                                                                                                                             +
         <attribute name="identifier" type="java.lang.String"/>                                                                                                             +
         <attribute name="description" type="java.lang.String"/>                                                                                                            +
         <attribute name="name" type="java.lang.String"/>                                                                                                                   +
         <collection name="pathways" referenced-type="Pathway" reverse-reference="pathwayInfo"/>                                                                            +
         <collection name="components" referenced-type="PathwayComponent" reverse-reference="pathwayInfo"/>                                                                 +
 </class>                                                                                                                                                                   +
 <class name="MRNA" extends="Transcript" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000234"></class>                                                      +
 <class name="MiRNA" extends="NcRNA" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000276"></class>                                                          +
 <class name="ChromosomalInversion" extends="ChromosomeStructureVariation" is-interface="true" term="http://purl.obolibrary.org/obo/SO:1000030"></class>                    +
 <class name="UTR" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000203">                                                          +
         <reference name="gene" referenced-type="Gene" reverse-reference="UTRs"/>                                                                                           +
         <collection name="transcripts" referenced-type="Transcript" reverse-reference="UTRs"/>                                                                             +
 </class>                                                                                                                                                                   +
 <class name="TFBindingSite" extends="BindingSite RegulatoryRegion" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000235">                                   +
         <reference name="CRM" referenced-type="CRM" reverse-reference="TFBindingSites"/>                                                                                   +
 </class>                                                                                                                                                                   +
 <class name="RRNA" extends="NcRNA" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000252"></class>                                                           +
 <class name="ChromosomalTranslocation" extends="SequenceFeature ChromosomeStructureVariation" is-interface="true" term="http://purl.obolibrary.org/obo/SO:1000044"></class>+
 <class name="Synonym" is-interface="true" term="http://semanticscience.org/resource/SIO_000122">                                                                           +
         <attribute name="value" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000122"/>                                                            +
         <reference name="subject" referenced-type="BioEntity" reverse-reference="synonyms"/>                                                                               +
         <collection name="dataSets" referenced-type="DataSet"/>                                                                                                            +
 </class>                                                                                                                                                                   +
 <class name="GeneFlankingRegion" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000239">                                           +
         <attribute name="direction" type="java.lang.String" term="http://purl.obolibrary.org/obo/PATO_0000039"/>                                                           +
         <attribute name="distance" type="java.lang.String" term="http://purl.obolibrary.org/obo/PATO_0000040"/>                                                            +
         <attribute name="includeGene" type="java.lang.Boolean"/>                                                                                                           +
         <reference name="gene" referenced-type="Gene" reverse-reference="flankingRegions"/>                                                                                +
 </class>                                                                                                                                                                   +
 <class name="CoexpressionJSON" is-interface="false">                                                                                                                       +
         <attribute name="highRange" type="java.lang.Float"/>                                                                                                               +
         <attribute name="JSON" type="java.lang.String"/>                                                                                                                   +
         <attribute name="lowRange" type="java.lang.Float"/>                                                                                                                +
         <reference name="gene" referenced-type="Gene"/>                                                                                                                    +
 </class>                                                                                                                                                                   +
 <class name="Oligo" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000696"></class>                                                +
 <class name="MeshTerm" is-interface="true" term="http://edamontology.org/data_0966">                                                                                       +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2099"/>                                                                          +
         <collection name="publications" referenced-type="Publication" reverse-reference="meshTerms"/>                                                                      +
 </class>                                                                                                                                                                   +
 <class name="ChromosomeBand" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000341"></class>                                       +
 <class name="OntologyAnnotation" is-interface="true" term="http://semanticscience.org/resource/SIO_001166">                                                                +
         <attribute name="qualifier" type="java.lang.String" term="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C41009"/>                                             +
         <reference name="subject" referenced-type="Annotatable" reverse-reference="ontologyAnnotations"/>                                                                  +
         <reference name="ontologyTerm" referenced-type="OntologyTerm" reverse-reference="ontologyAnnotations"/>                                                            +
         <collection name="dataSets" referenced-type="DataSet"/>                                                                                                            +
         <collection name="evidence" referenced-type="OntologyEvidence"/>                                                                                                   +
 </class>                                                                                                                                                                   +
 <class name="SOTerm" extends="OntologyTerm" is-interface="true" term="http://edamontology.org/data_0966"></class>                                                          +
 <class name="CrossReference" is-interface="true" term="http://semanticscience.org/resource/SIO_001171">                                                                    +
         <attribute name="identifier" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000675"/>                                                       +
         <reference name="source" referenced-type="DataSource"/>                                                                                                            +
         <reference name="subject" referenced-type="BioEntity" reverse-reference="crossReferences"/>                                                                        +
         <collection name="ontologyTerms" referenced-type="OntologyTerm" reverse-reference="xrefs"/>                                                                        +
 </class>                                                                                                                                                                   +
 <class name="OntologyTermSynonym" is-interface="true" term="http://semanticscience.org/resource/SIO_000122">                                                               +
         <attribute name="type" type="java.lang.String" term="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C25284"/>                                                  +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2099"/>                                                                          +
 </class>                                                                                                                                                                   +
 <class name="ProteinDomain" extends="BioEntity" is-interface="true">                                                                                                       +
         <attribute name="description" type="java.lang.String"/>                                                                                                            +
         <attribute name="type" type="java.lang.String"/>                                                                                                                   +
         <attribute name="shortName" type="java.lang.String"/>                                                                                                              +
         <collection name="proteins" referenced-type="Protein" reverse-reference="proteinDomains"/>                                                                         +
         <collection name="childFeatures" referenced-type="ProteinDomain"/>                                                                                                 +
         <collection name="foundIn" referenced-type="ProteinDomain"/>                                                                                                       +
         <collection name="parentFeatures" referenced-type="ProteinDomain"/>                                                                                                +
         <collection name="goAnnotation" referenced-type="GOAnnotation"/>                                                                                                   +
         <collection name="contains" referenced-type="ProteinDomain"/>                                                                                                      +
 </class>                                                                                                                                                                   +
 <class name="RNASeqExpression" is-interface="false">                                                                                                                       +
         <attribute name="countvariance" type="java.lang.Float"/>                                                                                                           +
         <attribute name="method" type="java.lang.String"/>                                                                                                                 +
         <attribute name="conflo" type="java.lang.Float"/>                                                                                                                  +
         <attribute name="count" type="java.lang.Float"/>                                                                                                                   +
         <attribute name="status" type="java.lang.String"/>                                                                                                                 +
         <attribute name="countdispersionvar" type="java.lang.Float"/>                                                                                                      +
         <attribute name="libraryExpressionLevel" type="java.lang.String"/>                                                                                                 +
         <attribute name="abundance" type="java.lang.Float"/>                                                                                                               +
         <attribute name="confhi" type="java.lang.Float"/>                                                                                                                  +
         <attribute name="locusExpressionLevel" type="java.lang.String"/>                                                                                                   +
         <attribute name="countuncertaintyvar" type="java.lang.Float"/>                                                                                                     +
         <reference name="experiment" referenced-type="RNASeqExperiment"/>                                                                                                  +
         <reference name="bioentity" referenced-type="BioEntity"/>                                                                                                          +
 </class>                                                                                                                                                                   +
 <class name="Ontology" is-interface="true" term="http://semanticscience.org/resource/SIO_001391">                                                                          +
         <attribute name="url" type="java.lang.String" term="http://edamontology.org/data_1052"/>                                                                           +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2099"/>                                                                          +
         <collection name="dataSets" referenced-type="DataSet"/>                                                                                                            +
 </class>                                                                                                                                                                   +
 <class name="FivePrimeUTR" extends="UTR" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000204"></class>                                                     +
 <class name="PathwaySVG" extends="java.lang.Object" is-interface="false">                                                                                                  +
         <attribute name="svg" type="java.lang.String"/>                                                                                                                    +
         <reference name="pathwayInfo" referenced-type="PathwayInfo"/>                                                                                                      +
 </class>                                                                                                                                                                   +
 <class name="Gene" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000704">                                                         +
         <attribute name="briefDescription" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000136"/>                                                 +
         <attribute name="description" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000136"/>                                                      +
         <attribute name="genomicOrder" type="java.lang.Integer"/>                                                                                                          +
         <reference name="upstreamIntergenicRegion" referenced-type="IntergenicRegion"/>                                                                                    +
         <reference name="downstreamIntergenicRegion" referenced-type="IntergenicRegion"/>                                                                                  +
         <reference name="alternateDescription" referenced-type="AlternateDescription" reverse-reference="gene"/>                                                           +
         <collection name="flankingRegions" referenced-type="GeneFlankingRegion" reverse-reference="gene"/>                                                                 +
         <collection name="rnaSeqEnrichments" referenced-type="RNASeqEnrichment" reverse-reference="bioentity"/>                                                            +
         <collection name="introns" referenced-type="Intron" reverse-reference="genes"/>                                                                                    +
         <collection name="proteins" referenced-type="Protein" reverse-reference="genes"/>                                                                                  +
         <collection name="CDSs" referenced-type="CDS" reverse-reference="gene"/>                                                                                           +
         <collection name="exons" referenced-type="Exon" reverse-reference="gene"/>                                                                                         +
         <collection name="pathways" referenced-type="Pathway" reverse-reference="genes"/>                                                                                  +
         <collection name="UTRs" referenced-type="UTR" reverse-reference="gene"/>                                                                                           +
         <collection name="transcripts" referenced-type="Transcript" reverse-reference="gene"/>                                                                             +
         <collection name="alleles" referenced-type="Allele" reverse-reference="gene"/>                                                                                     +
         <collection name="proteinFamily" referenced-type="ProteinFamily" reverse-reference="genes"/>                                                                       +
         <collection name="coexpressions" referenced-type="CoexpressionJSON" reverse-reference="gene"/>                                                                     +
         <collection name="regulatoryRegions" referenced-type="RegulatoryRegion" reverse-reference="gene"/>                                                                 +
         <collection name="goAnnotation" referenced-type="GOAnnotation"/>                                                                                                   +
 </class>                                                                                                                                                                   +
 <class name="TRNA" extends="NcRNA" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000253"></class>                                                           +
 <class name="Author" is-interface="true" term="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C42781">                                                                 +
         <attribute name="firstName" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000181"/>                                                        +
         <attribute name="initials" type="java.lang.String"/>                                                                                                               +
         <attribute name="lastName" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000182"/>                                                         +
         <attribute name="name" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000183"/>                                                             +
         <collection name="publications" referenced-type="Publication" reverse-reference="authors"/>                                                                        +
 </class>                                                                                                                                                                   +
 <class name="PathwayComponent" is-interface="true">                                                                                                                        +
         <attribute name="type" type="java.lang.String"/>                                                                                                                   +
         <attribute name="key" type="java.lang.String"/>                                                                                                                    +
         <attribute name="level" type="java.lang.Integer"/>                                                                                                                 +
         <attribute name="step" type="java.lang.Integer"/>                                                                                                                  +
         <reference name="pathwayInfo" referenced-type="PathwayInfo" reverse-reference="components"/>                                                                       +
         <collection name="ontologyTerms" referenced-type="OntologyTerm" reverse-reference="pathwayComponents"/>                                                            +
         <collection name="proteins" referenced-type="Protein" reverse-reference="pathwayComponents"/>                                                                      +
 </class>                                                                                                                                                                   +
 <class name="SequenceFeature" extends="BioEntity" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000110">                                                    +
         <attribute name="score" type="java.lang.Double" term="http://edamontology.org/data_1772"/>                                                                         +
         <attribute name="scoreType" type="java.lang.String" term="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C25284"/>                                             +
         <attribute name="length" type="java.lang.Integer" term="http://semanticscience.org/resource/SIO_000041"/>                                                          +
         <reference name="strain" referenced-type="Strain" reverse-reference="features"/>                                                                                   +
         <reference name="sequenceOntologyTerm" referenced-type="SOTerm"/>                                                                                                  +
         <reference name="chromosomeLocation" referenced-type="Location"/>                                                                                                  +
         <reference name="sequence" referenced-type="Sequence"/>                                                                                                            +
         <reference name="chromosome" referenced-type="Chromosome"/>                                                                                                        +
         <collection name="overlappingFeatures" referenced-type="SequenceFeature"/>                                                                                         +
         <collection name="childFeatures" referenced-type="SequenceFeature"/>                                                                                               +
 </class>                                                                                                                                                                   +
 <class name="ChromosomalDuplication" extends="ChromosomeStructureVariation" is-interface="true" term="http://purl.obolibrary.org/obo/SO:1000037"></class>                  +
 <class name="ThreePrimeUTR" extends="UTR" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000205"></class>                                                    +
 <class name="GOEvidenceCode" is-interface="true">                                                                                                                          +
         <attribute name="code" type="java.lang.String"/>                                                                                                                   +
 </class>                                                                                                                                                                   +
 <class name="Exon" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000147">                                                         +
         <reference name="gene" referenced-type="Gene" reverse-reference="exons"/>                                                                                          +
         <collection name="transcripts" referenced-type="Transcript" reverse-reference="exons"/>                                                                            +
 </class>                                                                                                                                                                   +
 <class name="SyntenyBlock" is-interface="true">                                                                                                                            +
         <collection name="syntenicRegions" referenced-type="SyntenicRegion" reverse-reference="syntenyBlock"/>                                                             +
         <collection name="dataSets" referenced-type="DataSet"/>                                                                                                            +
         <collection name="publications" referenced-type="Publication"/>                                                                                                    +
 </class>                                                                                                                                                                   +
 <class name="SnRNA" extends="NcRNA" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000274"></class>                                                          +
 <class name="Enhancer" extends="CRM" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000165"></class>                                                         +
 <class name="OntologyTerm" is-interface="true" term="http://semanticscience.org/resource/SIO_000275">                                                                      +
         <attribute name="identifier" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000675"/>                                                       +
         <attribute name="description" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000136"/>                                                      +
         <attribute name="obsolete" type="java.lang.Boolean" term="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C63553"/>                                             +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2099"/>                                                                          +
         <attribute name="namespace" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000067"/>                                                        +
         <reference name="ontology" referenced-type="Ontology"/>                                                                                                            +
         <collection name="parents" referenced-type="OntologyTerm"/>                                                                                                        +
         <collection name="crossReferences" referenced-type="OntologyTerm"/>                                                                                                +
         <collection name="pathwayComponents" referenced-type="PathwayComponent" reverse-reference="ontologyTerms"/>                                                        +
         <collection name="ontologyAnnotations" referenced-type="OntologyAnnotation" reverse-reference="ontologyTerm"/>                                                     +
         <collection name="xrefs" referenced-type="CrossReference" reverse-reference="ontologyTerms"/>                                                                      +
         <collection name="synonyms" referenced-type="OntologyTermSynonym"/>                                                                                                +
         <collection name="relations" referenced-type="OntologyRelation"/>                                                                                                  +
         <collection name="dataSets" referenced-type="DataSet"/>                                                                                                            +
 </class>                                                                                                                                                                   +
 <class name="OverlappingESTSet" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0001262">                                            +
         <collection name="ESTs" referenced-type="EST" reverse-reference="overlappingESTSets"/>                                                                             +
 </class>                                                                                                                                                                   +
 <class name="SequenceVariant" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0001060">                                                                        +
         <collection name="genes" referenced-type="Gene"/>                                                                                                                  +
 </class>                                                                                                                                                                   +
 <class name="ReversePrimer" extends="Primer" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000132"></class>                                                 +
 <class name="PathwayJSON" extends="java.lang.Object" is-interface="false">                                                                                                 +
         <attribute name="json" type="java.lang.String"/>                                                                                                                   +
         <reference name="pathwayInfo" referenced-type="PathwayInfo"/>                                                                                                      +
 </class>                                                                                                                                                                   +
 <class name="DataSource" is-interface="true">                                                                                                                              +
         <attribute name="description" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000136"/>                                                      +
         <attribute name="url" type="java.lang.String" term="http://edamontology.org/data_1052"/>                                                                           +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2099"/>                                                                          +
         <collection name="publications" referenced-type="Publication"/>                                                                                                    +
         <collection name="dataSets" referenced-type="DataSet" reverse-reference="dataSource"/>                                                                             +
 </class>                                                                                                                                                                   +
 <class name="Organism" is-interface="true" term="http://semanticscience.org/resource/SIO_010000">                                                                          +
         <attribute name="species" type="java.lang.String" term="http://edamontology.org/data_1045"/>                                                                       +
         <attribute name="genus" type="java.lang.String" term="http://edamontology.org/data_1870"/>                                                                         +
         <attribute name="taxonId" type="java.lang.String" term="http://edamontology.org/data_1179"/>                                                                       +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2909"/>                                                                          +
         <attribute name="version" type="java.lang.String"/>                                                                                                                +
         <attribute name="assemblyVersion" type="java.lang.String"/>                                                                                                        +
         <attribute name="annotationVersion" type="java.lang.String"/>                                                                                                      +
         <attribute name="commonName" type="java.lang.String" term="http://edamontology.org/data_2909"/>                                                                    +
         <attribute name="proteomeId" type="java.lang.Integer"/>                                                                                                            +
         <attribute name="shortName" type="java.lang.String" term="http://edamontology.org/data_2909"/>                                                                     +
         <collection name="strains" referenced-type="Strain"/>                                                                                                              +
         <collection name="pathways" referenced-type="Pathway" reverse-reference="organism"/>                                                                               +
 </class>                                                                                                                                                                   +
 <class name="OntologyAnnotationEvidenceCode" is-interface="true" term="http://purl.obolibrary.org/obo/ECO_0000000">                                                        +
         <attribute name="code" type="java.lang.String"/>                                                                                                                   +
         <attribute name="url" type="java.lang.String" term="http://edamontology.org/data_1052"/>                                                                           +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2099"/>                                                                          +
 </class>                                                                                                                                                                   +
 <class name="MicroarrayOligo" extends="Oligo" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000328"></class>                                                +
 <class name="PCRProduct" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000006"></class>                                           +
 <class name="Location" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000735">                                                                               +
         <attribute name="strand" type="java.lang.String" term="http://semanticscience.org/resource/SIO_001174"/>                                                           +
         <attribute name="start" type="java.lang.Integer" term="http://semanticscience.org/resource/SIO_000943"/>                                                           +
         <attribute name="end" type="java.lang.Integer" term="http://semanticscience.org/resource/SIO_000953"/>                                                             +
         <reference name="locatedOn" referenced-type="BioEntity" reverse-reference="locatedFeatures"/>                                                                      +
         <reference name="feature" referenced-type="BioEntity" reverse-reference="locations"/>                                                                              +
         <collection name="dataSets" referenced-type="DataSet"/>                                                                                                            +
 </class>                                                                                                                                                                   +
 <class name="PointMutation" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:1000008"></class>                                        +
 <class name="ChromosomeStructureVariation" extends="SequenceCollection" is-interface="true" term="http://purl.obolibrary.org/obo/SO:1000183"></class>                      +
 <class name="NaturalTransposableElement" extends="TransposableElement" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000797"></class>                       +
 <class name="ProteinAnalysisFeature" extends="BioEntity" is-interface="true">                                                                                              +
         <attribute name="significance" type="java.lang.Double"/>                                                                                                           +
         <attribute name="rawscore" type="java.lang.Double"/>                                                                                                               +
         <attribute name="programname" type="java.lang.String"/>                                                                                                            +
         <attribute name="normscore" type="java.lang.Double"/>                                                                                                              +
         <reference name="protein" referenced-type="Protein" reverse-reference="proteinAnalysisFeatures"/>                                                                  +
         <reference name="crossReference" referenced-type="CrossReference"/>                                                                                                +
 </class>                                                                                                                                                                   +
 <class name="AlternateDescription" is-interface="true">                                                                                                                    +
         <attribute name="value" type="java.lang.String"/>                                                                                                                  +
         <reference name="gene" referenced-type="Gene" reverse-reference="alternateDescription"/>                                                                           +
 </class>                                                                                                                                                                   +
 <class name="Transcript" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000673">                                                   +
         <attribute name="primaryTranscript" type="java.lang.Boolean"/>                                                                                                     +
         <reference name="gene" referenced-type="Gene" reverse-reference="transcripts"/>                                                                                    +
         <reference name="protein" referenced-type="Protein" reverse-reference="transcripts"/>                                                                              +
         <collection name="introns" referenced-type="Intron" reverse-reference="transcripts"/>                                                                              +
         <collection name="exons" referenced-type="Exon" reverse-reference="transcripts"/>                                                                                  +
         <collection name="CDSs" referenced-type="CDS" reverse-reference="transcript"/>                                                                                     +
         <collection name="UTRs" referenced-type="UTR" reverse-reference="transcripts"/>                                                                                    +
 </class>                                                                                                                                                                   +
 <class name="ForwardPrimer" extends="Primer" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000121"></class>                                                 +
 <class name="Protein" extends="BioEntity" is-interface="true" term="http://semanticscience.org/resource/SIO_010043">                                                       +
         <attribute name="md5checksum" type="java.lang.String" term="http://edamontology.org/data_2190"/>                                                                   +
         <attribute name="primaryAccession" type="java.lang.String" term="http://edamontology.org/data_2907"/>                                                              +
         <attribute name="molecularWeight" type="java.lang.Double" term="http://purl.bioontology.org/ontology/MESH/D008970"/>                                               +
         <attribute name="genbankIdentifier" type="java.lang.String"/>                                                                                                      +
         <attribute name="length" type="java.lang.Integer" term="http://semanticscience.org/resource/SIO_000041"/>                                                          +
         <reference name="sequence" referenced-type="Sequence"/>                                                                                                            +
         <collection name="CDSs" referenced-type="CDS" reverse-reference="protein"/>                                                                                        +
         <collection name="genes" referenced-type="Gene" reverse-reference="proteins"/>                                                                                     +
         <collection name="pathwayComponents" referenced-type="PathwayComponent" reverse-reference="proteins"/>                                                             +
         <collection name="pathways" referenced-type="Pathway" reverse-reference="proteins"/>                                                                               +
         <collection name="proteinFamily" referenced-type="ProteinFamilyMember" reverse-reference="protein"/>                                                               +
         <collection name="proteinDomains" referenced-type="ProteinDomain" reverse-reference="proteins"/>                                                                   +
         <collection name="proteinAnalysisFeatures" referenced-type="ProteinAnalysisFeature" reverse-reference="protein"/>                                                  +
         <collection name="transcripts" referenced-type="Transcript" reverse-reference="protein"/>                                                                          +
 </class>                                                                                                                                                                   +
 <class name="OntologyRelation" is-interface="true">                                                                                                                        +
         <attribute name="redundant" type="java.lang.Boolean"/>                                                                                                             +
         <attribute name="direct" type="java.lang.Boolean"/>                                                                                                                +
         <attribute name="relationship" type="java.lang.String"/>                                                                                                           +
         <reference name="parentTerm" referenced-type="OntologyTerm"/>                                                                                                      +
         <reference name="childTerm" referenced-type="OntologyTerm"/>                                                                                                       +
 </class>                                                                                                                                                                   +
 <class name="NcRNA" extends="Transcript" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000655"></class>                                                     +
 <class name="Pathway" is-interface="true">                                                                                                                                 +
         <attribute name="method" type="java.lang.String"/>                                                                                                                 +
         <attribute name="identifier" type="java.lang.String"/>                                                                                                             +
         <attribute name="status" type="java.lang.String"/>                                                                                                                 +
         <reference name="pathwayInfo" referenced-type="PathwayInfo" reverse-reference="pathways"/>                                                                         +
         <reference name="organism" referenced-type="Organism" reverse-reference="pathways"/>                                                                               +
         <collection name="genes" referenced-type="Gene" reverse-reference="pathways"/>                                                                                     +
         <collection name="proteins" referenced-type="Protein" reverse-reference="pathways"/>                                                                               +
 </class>                                                                                                                                                                   +
 <class name="BioEntity" extends="Annotatable" is-interface="true">                                                                                                         +
         <attribute name="symbol" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000105"/>                                                           +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2099"/>                                                                          +
         <attribute name="secondaryIdentifier" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000675"/>                                              +
         <reference name="organism" referenced-type="Organism"/>                                                                                                            +
         <collection name="rnaSeqExpressions" referenced-type="RNASeqExpression" reverse-reference="bioentity"/>                                                            +
         <collection name="locations" referenced-type="Location" reverse-reference="feature"/>                                                                              +
         <collection name="synonyms" referenced-type="Synonym" reverse-reference="subject"/>                                                                                +
         <collection name="crossReferences" referenced-type="CrossReference" reverse-reference="subject"/>                                                                  +
         <collection name="dataSets" referenced-type="DataSet" reverse-reference="bioEntities"/>                                                                            +
         <collection name="locatedFeatures" referenced-type="Location" reverse-reference="locatedOn"/>                                                                      +
 </class>                                                                                                                                                                   +
 <class name="RNASeqEnrichment" is-interface="false">                                                                                                                       +
         <attribute name="padj" type="java.lang.Float"/>                                                                                                                    +
         <attribute name="method" type="java.lang.String"/>                                                                                                                 +
         <attribute name="stat" type="java.lang.Float"/>                                                                                                                    +
         <attribute name="logChange" type="java.lang.Float"/>                                                                                                               +
         <attribute name="pvalue" type="java.lang.Float"/>                                                                                                                  +
         <reference name="experiment" referenced-type="RNASeqExperiment"/>                                                                                                  +
         <reference name="organism" referenced-type="Organism"/>                                                                                                            +
         <reference name="referenceExperiment" referenced-type="RNASeqExperiment"/>                                                                                         +
         <reference name="bioentity" referenced-type="BioEntity"/>                                                                                                          +
 </class>                                                                                                                                                                   +
 <class name="GOTerm" extends="OntologyTerm" is-interface="true"></class>                                                                                                   +
 <class name="DataSet" is-interface="true" term="http://semanticscience.org/resource/SIO_000089">                                                                           +
         <attribute name="description" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000136"/>                                                      +
         <attribute name="licence" type="java.lang.String" term="http://purl.org/dc/terms/license"/>                                                                        +
         <attribute name="url" type="java.lang.String" term="http://edamontology.org/data_1052"/>                                                                           +
         <attribute name="name" type="java.lang.String" term="http://edamontology.org/data_2099"/>                                                                          +
         <attribute name="version" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000653"/>                                                          +
         <reference name="dataSource" referenced-type="DataSource" reverse-reference="dataSets"/>                                                                           +
         <reference name="publication" referenced-type="Publication"/>                                                                                                      +
         <collection name="bioEntities" referenced-type="BioEntity" reverse-reference="dataSets"/>                                                                          +
 </class>                                                                                                                                                                   +
 <class name="Homolog" extends="java.lang.Object" is-interface="false">                                                                                                     +
         <attribute name="significance" type="java.lang.Double"/>                                                                                                           +
         <attribute name="method" type="java.lang.String"/>                                                                                                                 +
         <attribute name="score" type="java.lang.Double"/>                                                                                                                  +
         <attribute name="positives" type="java.lang.Integer"/>                                                                                                             +
         <attribute name="fromJGI" type="java.lang.Boolean"/>                                                                                                               +
         <attribute name="type" type="java.lang.String"/>                                                                                                                   +
         <attribute name="fromInParanoid" type="java.lang.Boolean"/>                                                                                                        +
         <attribute name="relationship" type="java.lang.String"/>                                                                                                           +
         <attribute name="tree" type="java.lang.String"/>                                                                                                                   +
         <attribute name="identities" type="java.lang.Integer"/>                                                                                                            +
         <attribute name="groupName" type="java.lang.String"/>                                                                                                              +
         <reference name="ortholog_gene" referenced-type="Gene"/>                                                                                                           +
         <reference name="gene" referenced-type="Gene"/>                                                                                                                    +
         <reference name="organism" referenced-type="Organism"/>                                                                                                            +
         <reference name="ortholog_organism" referenced-type="Organism"/>                                                                                                   +
 </class>                                                                                                                                                                   +
 <class name="ChromosomalDeletion" extends="ChromosomeStructureVariation" is-interface="true" term="http://purl.obolibrary.org/obo/SO:1000029"></class>                     +
 <class name="Comment" is-interface="true" term="http://semanticscience.org/resource/SIO_001167">                                                                           +
         <attribute name="description" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000136"/>                                                      +
         <attribute name="type" type="java.lang.String" term="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C25284"/>                                                  +
 </class>                                                                                                                                                                   +
 <class name="Chromosome" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000340"></class>                                           +
 <class name="OntologyEvidence" is-interface="true" term="http://purl.obolibrary.org/obo/ECO_0000000">                                                                      +
         <reference name="code" referenced-type="OntologyAnnotationEvidenceCode"/>                                                                                          +
         <collection name="publications" referenced-type="Publication"/>                                                                                                    +
 </class>                                                                                                                                                                   +
 <class name="RegulatoryRegion" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0005836">                                             +
         <reference name="gene" referenced-type="Gene" reverse-reference="regulatoryRegions"/>                                                                              +
 </class>                                                                                                                                                                   +
 <class name="CDNAClone" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000317"></class>                                            +
 <class name="BindingSite" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000409"></class>                                          +
 <class name="MSA" is-interface="true">                                                                                                                                     +
         <attribute name="primaryIdentifier" type="java.lang.String"/>                                                                                                      +
         <attribute name="HMM" type="java.lang.String"/>                                                                                                                    +
         <attribute name="alignment" type="java.lang.String"/>                                                                                                              +
 </class>                                                                                                                                                                   +
 <class name="RNASeqExperiment" is-interface="false">                                                                                                                       +
         <attribute name="description" type="java.lang.String"/>                                                                                                            +
         <attribute name="url" type="java.lang.String"/>                                                                                                                    +
         <attribute name="name" type="java.lang.String"/>                                                                                                                   +
         <attribute name="experimentGroup" type="java.lang.String"/>                                                                                                        +
         <reference name="organism" referenced-type="Organism"/>                                                                                                            +
 </class>                                                                                                                                                                   +
 <class name="SequenceCollection" extends="BioEntity" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0001260">                                                 +
         <reference name="sequenceOntologyTerm" referenced-type="SOTerm"/>                                                                                                  +
 </class>                                                                                                                                                                   +
 <class name="Sequence" is-interface="true" term="http://edamontology.org/data_2044">                                                                                       +
         <attribute name="md5checksum" type="java.lang.String" term="http://edamontology.org/data_2190"/>                                                                   +
         <attribute name="residues" type="org.intermine.objectstore.query.ClobAccess" term="http://edamontology.org/data_2044"/>                                            +
         <attribute name="length" type="int" term="http://semanticscience.org/resource/SIO_000041"/>                                                                        +
 </class>                                                                                                                                                                   +
 <class name="Publication" is-interface="true" term="http://semanticscience.org/resource/SIO_000087">                                                                       +
         <attribute name="year" type="java.lang.Integer"/>                                                                                                                  +
         <attribute name="issue" type="java.lang.String"/>                                                                                                                  +
         <attribute name="title" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000185"/>                                                            +
         <attribute name="pages" type="java.lang.String"/>                                                                                                                  +
         <attribute name="doi" type="java.lang.String" term="http://edamontology.org/data_1188"/>                                                                           +
         <attribute name="volume" type="java.lang.String"/>                                                                                                                 +
         <attribute name="journal" type="java.lang.String" term="http://semanticscience.org/resource/SIO_000160"/>                                                          +
         <attribute name="firstAuthor" type="java.lang.String" term="http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C42781"/>                                           +
         <attribute name="month" type="java.lang.String"/>                                                                                                                  +
         <attribute name="abstractText" type="java.lang.String" term="http://edamontology.org/data_2849"/>                                                                  +
         <attribute name="pubMedId" type="java.lang.String" term="http://edamontology.org/data_1187"/>                                                                      +
         <collection name="authors" referenced-type="Author" reverse-reference="publications"/>                                                                             +
         <collection name="entities" referenced-type="Annotatable" reverse-reference="publications"/>                                                                       +
         <collection name="meshTerms" referenced-type="MeshTerm" reverse-reference="publications"/>                                                                         +
 </class>                                                                                                                                                                   +
 <class name="ChromosomalTransposition" extends="ChromosomeStructureVariation" is-interface="true" term="http://purl.obolibrary.org/obo/SO:0000453"></class>                +
 <class name="CDS" extends="SequenceFeature" is-interface="true" term="http://purl.obolibrary.org/obo/SO_0000316">                                                          +
         <reference name="gene" referenced-type="Gene" reverse-reference="CDSs"/>                                                                                           +
         <reference name="transcript" referenced-type="Transcript" reverse-reference="CDSs"/>                                                                               +
         <reference name="protein" referenced-type="Protein" reverse-reference="CDSs"/>                                                                                     +
 </class>                                                                                                                                                                   +
 </model>
(1 row)

