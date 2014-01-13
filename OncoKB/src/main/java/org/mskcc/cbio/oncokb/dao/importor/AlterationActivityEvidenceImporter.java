/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mskcc.cbio.oncokb.dao.importor;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.mskcc.cbio.oncokb.bo.AlterationBo;
import org.mskcc.cbio.oncokb.bo.EvidenceBo;
import org.mskcc.cbio.oncokb.bo.GeneBo;
import org.mskcc.cbio.oncokb.model.Alteration;
import org.mskcc.cbio.oncokb.model.Evidence;
import org.mskcc.cbio.oncokb.model.EvidenceType;
import org.mskcc.cbio.oncokb.model.Gene;
import org.mskcc.cbio.oncokb.model.KnownEffectOfEvidence;
import org.mskcc.cbio.oncokb.util.ApplicationContextSingleton;
import org.mskcc.cbio.oncokb.util.FileUtils;
import org.mskcc.cbio.oncokb.util.GeneAnnotatorMyGeneInfo2;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author jgao
 */
public class AlterationActivityEvidenceImporter {
    private AlterationActivityEvidenceImporter() {
        throw new AssertionError();
    }
    
    private static String ALTERATION_ACTIVITY_EVIDENCE_FILE = "/data/alteration-activity-evidence.txt";
    
    public static void main(String[] args) throws IOException {
        List<String> lines = FileUtils.readLinesStream(
                AlterationActivityEvidenceImporter.class.getResourceAsStream(ALTERATION_ACTIVITY_EVIDENCE_FILE));
        String[] headers = lines.get(0).split("\t");
              
        ApplicationContext appContext = 
    		ApplicationContextSingleton.getApplicationContext();
	
    	GeneBo geneBo = GeneBo.class.cast(appContext.getBean("geneBo"));
    	AlterationBo alterationBo = AlterationBo.class.cast(appContext.getBean("alterationBo"));
    	EvidenceBo evidenceBo = EvidenceBo.class.cast(appContext.getBean("evidenceBo"));
        
        int nLines = lines.size();
        System.out.println("importing...");
        for (int i=1; i<nLines; i++) {
            String line = lines.get(i);
            String[] parts = line.split("\t");
            
            EvidenceType evidenceType = EvidenceType.ACTIVITY;
            String hugo = parts[0];
            String alt = parts[1];
            KnownEffectOfEvidence effect = KnownEffectOfEvidence.valueOf(parts[2]);
            String desc = parts[3];
            String context = parts[4];
            String tumorType = "";//parts[5];
            Set<String> pmids = new HashSet<String>(Arrays.asList(parts[6]));
            
            Gene gene = geneBo.findGeneByHugoSymbol(hugo);
            if (gene == null) {
                System.out.println("Could not find gene "+hugo+". Loading from MyGene.Info...");
                gene = GeneAnnotatorMyGeneInfo2.readByHugoSymbol(hugo);
                if (gene == null) {
                    System.out.println("Could not find gene "+hugo+" either. skip.");
                    continue;
                }
                geneBo.save(gene);
            }
            
            Alteration alteration = alterationBo.findAlteration(gene.getEntrezGeneId(), alt);
            if (alteration==null) {
                alteration = new Alteration(gene, alt, "Mutation");
                alterationBo.save(alteration);
            }
            
            Evidence evidence = new Evidence();
            evidence.setEvidenceType(evidenceType);
            evidence.setAlteration(alteration);
            evidence.setTumorType(null);
            evidence.setContext(context);
            evidence.setDescriptionOfKnownEffect(desc);
            evidence.setKnownEffect(effect);
            evidence.setPmids(pmids);
            
            evidenceBo.save(evidence);
        }
        
    }
}
