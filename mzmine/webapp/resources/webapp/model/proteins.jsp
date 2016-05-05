<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top" rowspan="2">
      <div class="heading2">
        Data sets
      </div>

  <c:set var="flagships">Branchiostoma floridae|Branchiostoma floridae|B. floridae,Ciona intestinalis|Ciona intestinalis|C. intestinalis,Nematostella vectensis|Nematostella vectensis|N. vectensis,Takifugu rubripes|Takifugu rubripes|T. rubripes,Xenopus tropicalis|Xenopus tropicalis|X. tropicalis</c:set>

  <c:set var="others">Aedes aegypti|Aedes aegypti|A. aegypti,Anopheles gambiae|mosquito|A. gambiae,Bombyx mori|Bombyx mori|B. mori,Caenorhabditis briggsae|Caenorhabditis briggsae|C. briggsae,Caenorhabditis elegans|worm|C. elegans,Canis familiaris|dog|C. familiaris,Ciona savignyi|Ciona savignyi|C. savignyi,Danio rerio|zebrafish|D. rerio,Drosophila melanogaster|fruitfly|D. melanogaster,Gallus gallus|Gallus gallus|G. gallus,Gasterosteus aculeatus|Gasterosteus aculeatus|G. aculeatus,Homo sapiens|human|H. sapiens,Hydra magnipapillata |Hydra magnipapillata|H. magnipapillata,Lottia gigantea|Lottia gigantea|L. gigantea,Monodelphis domestica|Monodelphis domestica|M. domestica,Mus musculus|mouse|M. musculus,Octopus bimaculoides|Octopus bimaculoides|O. bimaculoides,Oryzias latipes|Oryzias latipes|O. latipes,Rattus norvegicus|rat|R. norvegicus,Saccoglossus kowalevskii|Saccoglossus kowalevskii|S. kowalevskii,Strongylocentrotuspurpuratus |Strongylocentrotus purpuratus |S. purpuratus,Tribolium castaneum|Tribolium castaneum|T. castaneum</c:set>

      <div class="body">
        <p>
          Metazome contains annotated proteome data from the JGI flagship genomes:
          <ul>
           <c:forTokens items="${flagships}" delims="," var="flagship">

             <c:set var="field" value="${fn:split(flagship,'|')}" />
             <li>
               <em>${field[0]}</em>  - ${field[1]}
            <im:querylink text="Query for all proteins." skipBuilder="true">
                <query name="" model="genomic" view="Protein.primaryIdentifier Protein.organism.shortName Protein.sequence.residues" sortOrder="Protein.primaryIdentifier asc">
                  <constraint path="Protein.organism.shortName" op="=" value="${field[2]}"/>
                </query>
            </im:querylink>
             </li>
           </c:forTokens>
        </ul>

        As well as the proteomes of other JGI and model organism genomes
        <ul>

           <c:forTokens items="${others}" delims="," var="other">

             <c:set var="field" value="${fn:split(other,'|')}" />
             <li>
               <em>${field[0]}</em>  - ${field[1]}
            <im:querylink text="Query for all proteins." skipBuilder="true">
                <query name="" model="genomic" view="Protein.primaryIdentifier Protein.organism.shortName Protein.sequence.residues" sortOrder="Protein.primaryIdentifier asc">
                  <constraint path="Protein.organism.shortName" op="=" value="${field[2]}"/>
                </query>
            </im:querylink>
             </li>
           </c:forTokens>
         </ul>
      </div>
    </td>

    <td valign="top" width="40%">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
      Bulk data files for all organisms in Metazome are available for download from the 
      <a href="http://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=MetazomeV3">
      JGI Download Portal </a>.

      </div>
    </td>
  </tr>
</table>

