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

  <c:set var="genomes">Brachypodium distachyon ABR2|B. distachyon ABR2,Brachypodium distachyon ABR3|B. distachyon ABR3,Brachypodium distachyon ABR4|B. distachyon ABR4,Brachypodium distachyon ABR5|B. distachyon ABR5,Brachypodium distachyon ABR6|B. distachyon ABR6,Brachypodium distachyon ABR7|B. distachyon ABR7,Brachypodium distachyon ABR8|B. distachyon ABR8,Brachypodium distachyon ABR9|B. distachyon ABR9,Brachypodium distachyon Adi-10|B. distachyon Adi-10,Brachypodium distachyon Adi-12|B. distachyon Adi-12,Brachypodium distachyon Adi-2|B. distachyon Adi-2,Brachypodium distachyon Arn1|B. distachyon Arn1,Brachypodium distachyon Bd1-1|B. distachyon Bd1-1,Brachypodium distachyon Bd18-1|B. distachyon Bd18-1,Brachypodium distachyon Bd2-3|B. distachyon Bd2-3,Brachypodium distachyon Bd21-3|B. distachyon Bd21-3,Brachypodium distachyon Bd21Control|B. distachyon Bd21Control,Brachypodium distachyon Bd21Refv2|B. distachyon Bd21Refv2,Brachypodium distachyon Bd29-1|B. distachyon Bd29-1,Brachypodium distachyon Bd3-1|B. distachyon Bd3-1,Brachypodium distachyon Bd30-1|B. distachyon Bd30-1,Brachypodium distachyon BdTR10c|B. distachyon BdTR10c,Brachypodium distachyon BdTR11a|B. distachyon BdTR11a,Brachypodium distachyon BdTR11g|B. distachyon BdTR11g,Brachypodium distachyon BdTR11i|B. distachyon BdTR11i,Brachypodium distachyon BdTR12c|B. distachyon BdTR12c,Brachypodium distachyon BdTR13a|B. distachyon BdTR13a,Brachypodium distachyon BdTR13c|B. distachyon BdTR13c,Brachypodium distachyon BdTR1i|B. distachyon BdTR1i,Brachypodium distachyon BdTR2b|B. distachyon BdTR2b,Brachypodium distachyon BdTR2g|B. distachyon BdTR2g,Brachypodium distachyon BdTR3c|B. distachyon BdTR3c,Brachypodium distachyon BdTR5i|B. distachyon BdTR5i,Brachypodium distachyon BdTR7a|B. distachyon BdTR7a,Brachypodium distachyon BdTR8i|B. distachyon BdTR8i,Brachypodium distachyon BdTR9k|B. distachyon BdTR9k,Brachypodium distachyon Bis-1|B. distachyon Bis-1,Brachypodium distachyon Foz1|B. distachyon Foz1,Brachypodium distachyon Gaz-8|B. distachyon Gaz-8,Brachypodium distachyon Jer1|B. distachyon Jer1,Brachypodium distachyon Kah-1|B. distachyon Kah-1,Brachypodium distachyon Kah-5|B. distachyon Kah-5,Brachypodium distachyon Koz-1|B. distachyon Koz-1,Brachypodium distachyon Koz-3|B. distachyon Koz-3,Brachypodium distachyon Luc1|B. distachyon Luc1,Brachypodium distachyon Mig3|B. distachyon Mig3,Brachypodium distachyon Mon3|B. distachyon Mon3,Brachypodium distachyon Mur1|B. distachyon Mur1,Brachypodium distachyon Per1|B. distachyon Per1,Brachypodium distachyon Ron2|B. distachyon Ron2,Brachypodium distachyon S8iiC|B. distachyon S8iiC,Brachypodium distachyon Sig2|B. distachyon Sig2,Brachypodium distachyon Tek-2|B. distachyon Tek-2,Brachypodium distachyon Tek-4|B. distachyon Tek-4,Brachypodium distachyon Uni2|B. distachyon Uni2,Brachypodium distachyon pangenome|B. distachyon pangenome,Brachypodium distachyon|B. distachyon</c:set>


      <div class="body">
        <p>
          BrachyPan contains annotated proteome data from the JGI genomes:
          <ul>
           <c:forTokens items="${genomes}" delims="," var="genome">

             <c:set var="field" value="${fn:split(genome,'|')}" />
             <li>
               <em>${field[0]}</em>
            <im:querylink text="Query for all proteins." skipBuilder="true">
                <query name="" model="genomic" view="Protein.primaryIdentifier Protein.organism.shortName Protein.sequence.residues" sortOrder="Protein.primaryIdentifier asc">
                  <constraint path="Protein.organism.shortName" op="=" value="${field[1]}"/>
                </query>
            </im:querylink>
             </li>
           </c:forTokens>
        </ul>

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
      Bulk data files for all organisms in BrachyPan are available for download from the 
      <a href="https://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=BrachyPan">
      JGI Download Portal </a>.

      </div>
    </td>
  </tr>
</table>
