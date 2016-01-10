<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top" rowspan="2">
      <div class="heading2">
        Current data
      </div>
      <div class="body">
        <dl>
          <dt>mRNA Expression Data</dt>
          <dd>
            <p class="body">
            <a href="//www.phytozome.net">Phytozome</a> hosts  a collection of RNA-seq expression studies acquired from several internal and external sources.
            Next-generation sequencing reads are
            aligned to reference genomes, and gene- and transcript-level expression values are determined using Cufflinks.
            The set covered in Phytozome v11.0 includes:
	    <table class="zebra">
	     <thead>
	      <tr>
		<th class="organism">organism</th>
		<th class="gene-atlas">in Gene Atlas?</th>
		<th class="tissues-conditions">tissues / conditions</th>
		<th class="confidence"># of experiments</th>
	      </tr>
	     </thead>
	     <tbody>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Ahypochondriacus_er"><em>Amaranthus hypochondriacus</em></a></td>
		<td class="gene-atlas">no</td>
		<td>
		  <ul>
		    <li>Dry condition mixed</li>
		    <li>Floral</li>
		    <li>Green cotyledon</li>
		    <li>Immature seed</li>
		    <li>Maturing seed</li>
		    <li>Leaf</li>
		    <li>Root</li>
		    <li>Stem</li>
		  </ul>
		</td>
		<td>8, from the lab of <a href="//lifesciences.byu.edu/~pjm43">P. Jeff Maughan</a>, BYU</td>
	      </tr>	      
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Athaliana"><em>Arabidopsis thaliana</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		     <li>Root, leaf varied by nitrogen source</li>
		  </ul>
		</td>
		<td>6</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Bdistachyon"><em>Brachypodium distachyon</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Above ground tissues varied by light exposure</li>
		    <li>Root, leaf varied by nitrogen source</li>
		    <li>Young and mature leaf</li>
		    <li>Gradient young leaf tissues</li>
		    <li>Floral</li>
		    <li>Base, mixed and tip tissues of stem</li>
		  </ul>
		</td>
		<td>62</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Creinhardtii"><em>Chlamydomonas reinhardtii</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Anaerobiosis</li>
		    <li>Bilin signaling</li>
		    <li>Cu deficiency photoautotrophic</li>
		    <li>Fe deficiency</li>
		    <li>H<sub>2</sub>O<sub>2</sub> treatment</li>
		    <li>N concentration</li>
		    <li>Heterotrophic, photoheterotrophic and phototrophic cultures varied by light exposure, nitrogen source and harvesting time</li>
		  </ul>
		</td>
		<td>133</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Egrandis"><em>Eucalyptus grandis</em></a></td>
		<td class="gene-atlas">no</td>
		<td>
		  <ul>
		    <li>Mature leaf</li>
		    <li>Young leaf</li>
		    <li>Phloem</li>
		    <li>Shoot tips</li>
		    <li>Xylem</li>
		    <li>Immature xylem</li>
		  </ul>
		</td>
		<td>6, performed on a <em>E. grandis</em> &times; <em>E. urophylla</em> hybrid</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Gmax"><em>Glycine max</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Root, lateral root, root tip, root hairs, nodules, shoot tip, apical meristem shoot, leaf, flower, seed</li>  
		    <li>Root, leaf varied by nitrogen source</li>
		    <li>Opened, unopened flower</li>
		    <li>Symbiotic conditions for root, nodules, leaf</li>
		  </ul>
		</td>
		<td>26</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Mtruncatula"><em>Medicago truncatula</em><a/></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Root, leaf varied by nitrogen source</li>
		    <li>Symbiotic conditions for root, nodules, leaf</li>
		  </ul>
		</td>
		<td>9</td>
	      </tr>	      
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Phallii_er"><em>Panicum hallii</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Root, leaf varied by nitrogen source</li>
		  </ul>
		</td>
		<td>9</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Pvirgatum"><em>Panicum virgatum</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Stem varied by nitrogen source</li>
		    <li>Leaf blade, sheath</li>
		    <li>Vascular bundle</li>
		    <li>Root, nodes, crown, floret</li> 
		    <li>Inflorescence: emerging panicle, elongating rachis branch</li>
		    <li>Seed varied by length of germination</li> 
		  </ul>
		</td>
		<td>26</td>
	      </tr>
	      <tr>		
		<td><a href="/pz/portal.html#!info?alias=Org_Pvulgaris"><em>Phaseolus vulgaris</em></a></td>
		<td class="gene-atlas">no</td>
		<td>
		<ul>
		  <li>Root, nodule, stem, leaf</li>
		  <li>Young pod, green mature pod</li>
		  <li>Budding and mature flower</li>
		</ul>
		</td>
		<td>11</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Ppatens"><em>Physcomitrella patens</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Gametophores, varied by medium, (de/re)hydration, treatment with NAA</li>
		    <li>Detached leaflets from gametophores</li>
		    <li>Protonema, varied by medium with wide variety of treatments, light conditions</li>
		    <li>Protoplasts from protonema</li>
		    <li>Spores</li>
		    <li>Green and brown sporophytes</li>
		  </ul>
		</td>
		<td>32</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Ptrichocarpa"><em>Populus trichocarpa</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Root, root tip, stem node and internode</li>
		    <li>Root and stem varied by nitrogen source</li>
		    <li>Bud, leaf, male and female flowers varied by maturity</li>
		  </ul>
		</td>
		<td>24</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Sitalica"><em>Setaria italica</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Etiolated seedling, germinating root, germinating shoot</li>
		    <li>Multiple runs of leaf tissue in high light</li>
		    <li>Root, shoot, panicle</li>
		    <li>Root varied by nitrogen source, amount of H<sub>2</sub>O</li>
		    <li>Mixed tissue varied by light condition</li>
		  </ul>
		</td>
		<td>20</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Sviridis_er"><em>Setaria viridis</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		  <ul>
		    <li>Etiolated germinating shoot</li>
		    <li>Multiple runs of leaf tissue in high light</li>
		    <li>Root, tiller, shoot, panicle</li>
		    <li>Root varied by nitrogen source, amount of H<sub>2</sub>O</li>
		    <li>Mixed tissue varied by light condition</li>		  
		  </ul>
		</td>
		<td>19</td>
	      </tr>
	      <tr>
		<td><a href="/pz/portal.html#!info?alias=Org_Sbicolor_er"><em>Sorghum bicolor</em></a></td>
		<td class="gene-atlas">yes</td>
		<td>
		 <ul>
		  <li>Wide variety of root, stem and leaf structures, panicle and peduncle when juvenile, vegetative and at anthesis, floral initiation and grain maturity</li>
		  <li>Dry and imbibed seed at grain maturity</li>
		  <li>Root, shoot varied by nitrogen source, control with full fertilization, with H<sub>2</sub>O only</li>
		 </ul>	    
		</td>
		<td>47</td>
	      </tr>	      
	     </tbody> 
	    </table>
            </p>
            <p class="body">
            Please note this includes some data from the initial release of JGI Plant Gene Atlas Project:
            <ul>
              <li><em>Chlamydomonas reinhardtii</em> (26 unpublished experiments from <a href="http://www.chem.ucla.edu/dept/Faculty/merchant/">Sabeeha Merchant</a>, email: <span class="monospace">merchant AT chem DOT ucla DOT edu</span>)</li>
              <li><em>Physcomitrella patens</em> (unpublished data from <a href="http://old-site.bioss-freiburg.de/www.bioss.uni-freiburg.de/cms/rensing.html">Stefan Rensing</a>, email: <span class="monospace">stefan DOT rensing AT biologie DOT uni-marburg DOT de</span>)</li>
              <li><em>Populus trichocarpa</em> (unpublished data from <a href="http://www.esd.ornl.gov/PGG/tuskan_bio.htm">Gerald Tuskan</a>, email: <span class="monospace">gtk AT ornl DOT gov</a>)</li>
            </ul>
            </p>

            <p class="body">
            This project is a multi-laboratory collaboration that seeks to produce a standardized
            expression atlas across diverse tissues and time courses from JGI Plant
            Flagship organisms.  More detail about the Plant Flagship Genomes can be
            found
     <a href="http://jgi.doe.gov/our-science/science-programs/plant-genomics/plant-flagship-genomes">
            here.</a>
            JGI users can submit proposals to have tissues and conditions of
            interest included in the Plant Atlas through the 
            <a href="http://jgi.doe.gov/collaborate-with-jgi/community-science-program/">
            JGI Community Science Program
            </a>.
            </p>

            <p class="body">
            This v0.1 release provides normalized expression values for diverse
	    growth conditions of <em>Chlamydomonas</em>, <em>Populus</em>, and <em>Physcomitrella</em>.
	    </p>
	    
            <p class="body">
            This data is unpublished.  As a public service, the
            Department of Energy's Joint Genome Institute (JGI) is
            making the RNA-seq expression data
            available before scientific publication according to the
            Ft. Lauderdale Accord. This balances the imperative of the
            DOE and the JGI that the data from its sequencing projects
            be made available as soon and as completely as possible
            with the desire of contributing scientists and the JGI to
            reserve a reasonable period of time to publish on analysis without concerns about preemption
            by other groups.
            </p>
	        <p class="body">
            At the present time, we request that anyone intending to download Gene Atlas data for use in any analysis to <b>please contact the overall project lead</b>,
	    <a href="http://www.hagsc.org/about/jschmutz">Jeremy Schmutz</a> at JGI/HudsonAlpha, email: <span class="monospace">jschmutz AT hudsonalpha DOT org</span>.   To be kept informed on the progress of the JGI Gene Atlas project, changes in 
	   the data usage policy, or to ask questions, please subscribe to the project mailing list by sending a blank email with the Subject line <span class="monospace">subscribe jgi-gene_atlas FirstName LastName</span>
	   to <span class="monospace">sympa@lists.lbl.gov</span>.
            </p>

          </dd>
        </dl>
      </div>
    </td>
    <td valign="top">
      <div class="heading2">
        Bulk download
      </div>
      <div class="body">
      Bulk data files for gene expression data for <em><b>non-Gene Atlas</b></em> experiments are available for download from individual organisms folders at the 
      <a href="http://genome.jgi.doe.gov/pages/dynamicOrganismDownload.jsf?organism=PhytozomeV10">
      JGI Genome Portal </a>.
      </div>
    </td>
  </tr>
</table>
