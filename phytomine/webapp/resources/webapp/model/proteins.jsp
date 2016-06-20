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

  <c:set var="flagships">Glycine max|soybean|G. max,Setaria italica|foxtail millet|S. italica,Populus trichocarpa|western poplar|P. trichocarpa,Physcomitrella patens|moss|P. patens,Chlamydomonas reinhardtii|green algae|C. reinhardtii,Brachypodium distachyon|purple false brome|B. distachyon,Panicum virgatum early-release|switchgrass|P. virgatum,Panicum virgatum|switchgrass|P. virgatum early-release,Sorghum bicolor|cereal grass|S. bicolor</c:set>

  <c:set var="others"> Amaranthus hypochondriacus|grain amaranth|A. hypochondriacus early-release,Amborella trichopoda|amborella|A. trichopoda,Ananas comosus|pineapple|A. comosus early-release,Aquilegia coerulea early-release|Colorado blue columbine|A. coerulea early-release,Aquilegia coerulea|Colorado blue columbine|A. coerulea,Arabidopsis halleri|Arabidopsis halleri|A. halleri early-release,Arabidopsis lyrata|Lyrate rockcress|A. lyrata,Arabidopsis thaliana|thale cress|A. thaliana,Boechera stricta|Drummond's rockcress|B. stricta,Brachypodium stacei|Brachypodium stacei|B. stacei,Brassica rapa FPsc|turnip mustard|B. rapa FPsc,Capsella grandiflora|Capsella grandiflora|C. grandiflora,Capsella rubella|red shepherd's purse|C. rubella,Carica papaya|papaya|C. papaya,Citrus clementina|clementine|C. clementina,Citrus sinensis|sweet orange|C. sinensis,Coccomyxa subellipsoidea C-169 |Coccomyxa subellipsoidea C-169 |C. subellipsoidea C-169,Cucumis sativus|cucumber|C. sativus,Daucus carota|carrots|D. carota early-release,Dunaliella salina|D. salina|D. salina early-release Eucalyptus grandis|eucalyptus|E. grandis,Eutrema salsugineum|salt cress|E. salsugineum,Fragaria vesca|strawberry|F. vesca,Gossypium raimondii|cotton|G. raimondii,Kalanchoe laxiflora|K. laxiflora|K. laxiflora early-release,Kalanchoe marnieriana|Kalanchoe marnieriana|K. marnieriana,Linum usitatissimum|flax|L. usitatissimum,Malus domestica|apple|M. domestica,Manihot esculenta|cassava|M. esculenta,Medicago truncatula|barrel medic|M. truncatula,Micromonas pusilla CCMP1545|Micromonas pusilla CCMP1545|M. pusilla CCMP1545,Micromonas sp. RCC299|Micromonas sp. RCC299|M. sp. RCC299,Mimulus guttatus|monkey flower|M. guttatus,Musa acuminata|banana|M. acuminata,Oropetium thomaeum|O. thomaeum|O. thomaeum early-release,Oryza sativa|rice|O. sativa,Ostreococcus lucimarinus|Ostreococcus lucimarinus|O. lucimarinus,Panicum hallii|Hall's panicgrass|P. hallii,Phaseolus vulgaris|common bean|P. vulgaris,Prunus persica|peach|P. persica,Ricinus communis|castor bean plant|R. communis,Salix purpurea|shrub willow|S. purpurea,Selaginella moellendorffii|spikemoss|S. moellendorffii,Setaria viridis|Setaria viridis|S. viridis,Solanum lycopersicum|tomato|S. lycopersicum,Solanum tuberosum|potato|S. tuberosum,Sphagnum fallax|Sphagnum fallax|S. fallax,Spirodela polyrhiza|greater duckweed|S. polyrhiza,Theobroma cacao|cocoa bean|T. cacao,Trifolium pratense|red clover|T. pratense early-release,Triticum aestivum|wheat|T. aestivum early-release,Vitis vinifera|grape vine|V. vinifera,Volvox carteri|volvox|V. carteri,Zea mays|maize|Z. mays,Zostera marina|common eelgrass|Z. marina early-release,</c:set>

      <div class="body">
        <p>
          Phytomine contains annotated proteome data from the JGI flagship genomes:
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
      Bulk data files for all organisms in Phytozome are available for download from the 
      <a href="http://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=PhytozomeV10">
      JGI Download Portal </a>.

      </div>
    </td>
  </tr>
</table>
