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


  <c:set var="flagships">Alyssum linifolium|Alyssum linifolium|A. linifolium,Cakile maritima|Cakile maritima|C. maritima,Capsella rubella|red shepherd's purse|C. rubella,Caulanthus amplexicaulis|Caulanthus amplexicaulis|C. amplexicaulis,Cleome violacea|Cleome violacea|C. violacea,Crambe hispanica|Crambe hispanica|C. hispanica,Descurainia sophioides|Descurainia sophioides|D. sophioides,Diptychocarpus strictus|Diptychocarpus strictus|D. strictus,Eruca vesicaria|Eruca vesicaria|E. vesicaria,Euclidium syriacum|Euclidium syriacum|E. syriacum,Iberis amara|Iberis amara|I. amara,Isatis tinctoria|Isatis tinctoria|I. tinctoria,Lepidium sativum|Lepidium sativum|L. sativum,Lunaria annua|Lunaria annua|L. annua,Malcolmia maritima|Malcolmia maritima|M. maritima,Myagrum perfoliatum|Myagrum perfoliatum|M. perfoliatum,Rorippa islandica|Rorippa islandica|R. islandica,Sinapis alba|Sinapis alba|S. alba,Stanleya pinnata|Stanleya pinnata|S. pinnata,Thlaspi arvense|Thlaspi arvense|T. arvense</c:set>


      <div class="body">
        <p>
          Phytomine contains annotated proteome data from the JGI genomes:
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

      </div>
    </td>

    <td valign="top" width="40%">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
      Bulk data files for all organisms in BMAPmine are available for download from the 
      <a href="http://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=BMAP">
      JGI Download Portal </a>.

      </div>
    </td>
  </tr>
</table>
