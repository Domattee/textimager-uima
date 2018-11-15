package org.hucompute.textimager.uima.microNER;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.json.JSONArray;
import org.json.JSONObject;

import de.tudarmstadt.ukp.dkpro.core.api.io.IobDecoder;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.resources.MappingProvider;

@TypeCapability(
		inputs = {
				"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
		"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",},
		outputs = {"de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency"})




public class NERTagger extends JCasAnnotator_ImplBase {
	
	public static final String PARAM_NAMED_ENTITY_MAPPING_LOCATION = 
            ComponentParameters.PARAM_NAMED_ENTITY_MAPPING_LOCATION;
    @ConfigurationParameter(name = PARAM_NAMED_ENTITY_MAPPING_LOCATION, mandatory = false)
    private String namedEntityMappingLocation;

	private MappingProvider namedEntityMappingProvider;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		
		namedEntityMappingProvider = new MappingProvider();
        namedEntityMappingProvider.setDefault(MappingProvider.LOCATION, "/home/s3035016/eclipse-workspace/textimager-uima/textimager-uima-mNER/src/main/resources/ner-default.map");
        namedEntityMappingProvider.setDefault(MappingProvider.BASE_TYPE, NamedEntity.class.getName());
//        namedEntityMappingProvider.setOverride(MappingProvider.LOCATION, namedEntityMappingLocation);
        namedEntityMappingProvider.setOverride(MappingProvider.LANGUAGE, "de");
		
	}
	
	// IP and Port of the server running the flask parser client
	private String restEndpointBase = "http://127.0.0.1:5000";
	// Request to run parser function
	private String restEndpointVerb = "api";
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		
		namedEntityMappingProvider.configure(aJCas.getCas());
		System.out.println("Building Input String...");
		// Build the conllu format string that gets send to the parser. Cas has to contain: Tokens, Lemma, xpos and morphs.
		StringBuilder builder = new StringBuilder();
		builder.append("{ \"meta\" : { \"model\" : \"germeval-conll.h5\" }, \"data\" : { \"tokens\" : [");
		int size = JCasUtil.select(aJCas, Sentence.class).size();
		int counter = 0;
		for (Sentence sentence: JCasUtil.select(aJCas, Sentence.class)) {
			counter += 1;
			builder.append("[\"");
			
			List<Token> tokens = JCasUtil.selectCovered(Token.class, sentence);
			
			builder.append(tokens.stream().map(token -> token.getCoveredText()).collect(Collectors.joining("\", \"")));
			
			//for (Token token : JCasUtil.selectCovered(Token.class, sentence)) {
			//	builder.append(token.getCoveredText());
			//	builder.append(",");
			//Now arrange them all in the proper format somehow. Manage sentence boundaries too.
			builder.append("\"]");
			if (counter < size) builder.append(", ");
			System.out.println(String.valueOf(counter) + ", " + String.valueOf(size));
		}
		builder.append("], \"sentences\" : [\"This is a dummy sentence\"]}}");
		
		String input = builder.toString();
		System.out.println(input);
		
		// Test string
		//String input = "1\t„\t„\tX\tXY\t_\t_\t_\t_\tSpaceAfter=No\n2\tIch\tich\tPRON\tPPER\tcase=nom|number=sg|gender=*|person=1|prontype=prs\t_\t_\t_\t_\n3\tmöchte\tmöchten\tVERB\tVMFIN\tnumber=sg|person=1|tense=pres|mood=ind|verbform=fin|verbtype=mod\t_\t_\t_\t_\n4\teuch\teuch\tPRON\tPPER\tcase=dat|number=pl|gender=*|person=2|prontype=prs\t_\t_\t_\t_\n5\tungern\tunger\tADV\tADV\t_\t_\t_\t_\t_\n6\tunter\tunter\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n7\tDruck\tDruck\tNOUN\tNN\tcase=acc|number=sg|gender=masc\t_\t_\t_\t_\n8\tsetzen\tsetzen\tVERB\tVVINF\tverbform=inf\t_\t_\t_\tSpaceAfter=No\n9\t,\t--\tPUNCT\t$,\tpuncttype=comm\t_\t_\t_\t_\n10\taber\taber\tCONJ\tKON\t_\t_\t_\t_\t_\n11\tdas\tder\tDET\tART\tcase=nom|number=sg|gender=neut|prontype=art\t_\t_\t_\t_\n12\tSchicksal\tSchicksal\tNOUN\tNN\tcase=nom|number=sg|gender=neut\t_\t_\t_\t_\n13\tder\tder\tDET\tART\tcase=gen|number=sg|gender=fem|prontype=art\t_\t_\t_\t_\n14\tRepublik\tRepublik\tNOUN\tNN\tcase=gen|number=sg|gender=fem\t_\t_\t_\t_\n15\tliegt\tliegen\tVERB\tVVFIN\tnumber=sg|person=3|tense=pres|mood=ind|verbform=fin\t_\t_\t_\t_\n16\tin\tin\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n17\teuren\teur\tDET\tPPOSAT\tcase=dat|number=pl|gender=fem|poss=yes|prontype=prs\t_\t_\t_\t_\n18\tHänden\tHand\tNOUN\tNN\tcase=dat|number=pl|gender=fem\t_\t_\t_\tSpaceAfter=No\n19\t“\t“\tPART\tPTKVZ\tparttype=vbp\t_\t_\t_\tSpaceAfter=No\n20\t,\t--\tPUNCT\t$,\tpuncttype=comm\t_\t_\t_\t_\n21\tsagte\tsagen\tVERB\tVVFIN\tnumber=sg|person=3|tense=past|mood=ind|verbform=fin\t_\t_\t_\t_\n22\ter\ter\tPRON\tPPER\tcase=nom|number=sg|gender=masc|person=3|prontype=prs\t_\t_\t_\t_\n23\tzu\tzu\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n24\tder\tder\tDET\tART\tcase=dat|number=sg|gender=fem|prontype=art\t_\t_\t_\t_\n25\tMenschenmenge\tMenschenmenge\tNOUN\tNN\tcase=dat|number=sg|gender=fem\t_\t_\t_\tSpaceAfter=No\n26\t,\t--\tPUNCT\t$,\tpuncttype=comm\t_\t_\t_\t_\n27\tdie\tder\tPRON\tPRELS\tcase=nom|number=sg|gender=fem|prontype=rel\t_\t_\t_\t_\n28\tsich\tsich\tPRON\tPRF\tcase=acc|number=sg|person=3|prontype=prs|reflex=yes\t_\t_\t_\t_\n29\tauf\tauf\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n30\teinem\tein\tDET\tART\tcase=dat|number=sg|gender=masc|prontype=art\t_\t_\t_\t_\n31\tSportplatz\tSportplatz\tNOUN\tNN\tcase=dat|number=sg|gender=masc\t_\t_\t_\t_\n32\tan\tan\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n33\tder\tder\tDET\tART\tcase=dat|number=sg|gender=fem|prontype=art\t_\t_\t_\t_\n34\tUniversity\tUniversity\tPROPN\tNE\tcase=dat|number=sg|gender=fem\t_\t_\t_\t_\n35\tof\tof\tPROPN\tNE\tcase=*|number=*|gender=*\t_\t_\t_\t_\n36\tNorth\tNorth\tPROPN\tNE\tcase=*|number=*|gender=*\t_\t_\t_\t_\n37\tCarolina\tCarolina\tPROPN\tNE\tcase=nom|number=sg|gender=neut\t_\t_\t_\t_\n38\tversammelt\tversammeln\tVERB\tVVPP\taspect=perf|verbform=part\t_\t_\t_\t_\n39\thatte\thaben\tAUX\tVAFIN\tnumber=sg|person=3|tense=past|mood=ind|verbform=fin\t_\t_\t_\tSpaceAfter=No\n40\t.\t--\tPUNCT\t$.\tpuncttype=peri\t_\t_\t_\t_\n\n1\tDie\tder\tDET\tART\tcase=nom|number=pl|gender=fem|prontype=art\t_\t_\t_\t_\n2\tneuen\tneu\tADJ\tADJA\tcase=nom|number=pl|gender=fem|degree=pos\t_\t_\t_\t_\n3\tAusgaben\tAusgabe\tNOUN\tNN\tcase=nom|number=pl|gender=fem\t_\t_\t_\t_\n4\twerden\twerden\tAUX\tVAFIN\tnumber=pl|person=3|tense=pres|mood=ind|verbform=fin\t_\t_\t_\t_\n5\tüber\tüber\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n6\tClintons\tClinton\tPROPN\tNE\tcase=gen|number=sg|gender=*\t_\t_\t_\t_\n7\twohlgefülltes\twohlgefüllt\tADJ\tADJA\tcase=acc|number=sg|gender=neut|degree=pos\t_\t_\t_\t_\n8\tBankkonto\tBankkonto\tNOUN\tNN\tcase=acc|number=sg|gender=neut\t_\t_\t_\t_\n9\tfinanziert\tfinanzieren\tVERB\tVVPP\taspect=perf|verbform=part\t_\t_\t_\tSpaceAfter=No\n10\t.\t--\tPUNCT\t$.\tpuncttype=peri\t_\t_\t_\t_\n\n1\tWas\twas\tPRON\tPWS\tcase=acc|number=sg|gender=neut|prontype=int\t_\t_\t_\t_\n2\tsie\tsie\tPRON\tPPER\tcase=nom|number=sg|gender=fem|person=3|prontype=prs\t_\t_\t_\t_\n3\tsagt\tsagen\tVERB\tVVFIN\tnumber=sg|person=3|tense=pres|mood=ind|verbform=fin\t_\t_\t_\t_\n4\tund\tund\tCONJ\tKON\t_\t_\t_\t_\t_\n5\twas\twas\tPRON\tPWS\tcase=acc|number=sg|gender=neut|prontype=int\t_\t_\t_\t_\n6\tsie\tsie\tPRON\tPPER\tcase=nom|number=sg|gender=fem|person=3|prontype=prs\t_\t_\t_\t_\n7\ttut\ttun\tVERB\tVVFIN\tnumber=sg|person=3|tense=pres|mood=ind|verbform=fin\t_\t_\t_\t_\n8\t-\t--\tPUNCT\t$(\tpuncttype=brck\t_\t_\t_\t_\n9\teigentlich\teigentlich\tADV\tADV\t_\t_\t_\t_\t_\n10\tist\tsein\tAUX\tVAFIN\tnumber=sg|person=3|tense=pres|mood=ind|verbform=fin\t_\t_\t_\t_\n11\tes\tes\tPRON\tPPER\tcase=nom|number=sg|gender=neut|person=3|prontype=prs\t_\t_\t_\t_\n12\tunglaublich\tunglaublich\tADJ\tADJD\tdegree=pos|variant=short\t_\t_\t_\tSpaceAfter=No\n13\t.\t--\tPUNCT\t$.\tpuncttype=peri\t_\t_\t_\t_\n\n1\t5.000\t5.000\tNUM\tCARD\tnumtype=card\t_\t_\t_\t_\n2\t$\t$\tNOUN\tNN\tcase=*|number=*|gender=masc\t_\t_\t_\t_\n3\tpro\tpro\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n4\tPerson\tPerson\tNOUN\tNN\tcase=acc|number=sg|gender=fem\t_\t_\t_\tSpaceAfter=No\n5\t,\t--\tPUNCT\t$,\tpuncttype=comm\t_\t_\t_\t_\n6\tdas\tder\tDET\tART\tcase=acc|number=sg|gender=neut|prontype=art\t_\t_\t_\t_\n7\terlaubte\terlauben\tADJ\tADJA\tcase=acc|number=sg|gender=neut|degree=pos\t_\t_\t_\t_\n8\tMaximum\tMaximum\tNOUN\tNN\tcase=acc|number=sg|gender=neut\t_\t_\t_\tSpaceAfter=No\n9\t.\t--\tPUNCT\t$.\tpuncttype=peri\t_\t_\t_\t_\n\n1\tAnfang\tAnfang\tNOUN\tNN\tcase=*|number=sg|gender=masc\t_\t_\t_\t_\n2\tOktober\tOktober\tNOUN\tNN\tcase=*|number=sg|gender=masc\t_\t_\t_\t_\n3\tnutzte\tnutzen\tVERB\tVVFIN\tnumber=sg|person=3|tense=past|mood=ind|verbform=fin\t_\t_\t_\t_\n4\tdas\tder\tDET\tART\tcase=nom|number=sg|gender=neut|prontype=art\t_\t_\t_\t_\n5\tÜbergangsteam\tÜbergangsteam\tNOUN\tNN\tcase=nom|number=sg|gender=neut\t_\t_\t_\t_\n6\tdenselben\tderselbe\tDET\tPDAT\tcase=acc|number=sg|gender=masc|prontype=dem\t_\t_\t_\t_\n7\tVeranstaltungsort\tVeranstaltungsort\tNOUN\tNN\tcase=acc|number=sg|gender=masc\t_\t_\t_\t_\n8\tfür\tfür\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n9\tein\tein\tDET\tART\tcase=acc|number=sg|gender=neut|prontype=art\t_\t_\t_\t_\n10\tTreffen\tTreffen\tNOUN\tNN\tcase=acc|number=sg|gender=neut\t_\t_\t_\t_\n11\tmit\tmit\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n12\tTechnologielobbyisten\tTechnologielobbyist\tNOUN\tNN\tcase=dat|number=pl|gender=masc\t_\t_\t_\tSpaceAfter=No\n13\t,\t--\tPUNCT\t$,\tpuncttype=comm\t_\t_\t_\t_\n14\tzu\tzu\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n15\tdem\tder\tDET\tART\tcase=dat|number=sg|gender=masc|prontype=art\t_\t_\t_\t_\n16\tu.a.\tu.a.\tADJ\tADJA\tcase=dat|number=sg|gender=masc|degree=pos\t_\t_\t_\t_\n17\tVertreter\tVertreter\tNOUN\tNN\tcase=dat|number=sg|gender=masc\t_\t_\t_\t_\n18\tvon\tvon\tADP\tAPPR\tadptype=prep\t_\t_\t_\t_\n19\tUber\tUber\tPROPN\tNE\tcase=dat|number=sg|gender=masc\t_\t_\t_\tSpaceAfter=No\n20\t,\t--\tPUNCT\t$,\tpuncttype=comm\t_\t_\t_\t_\n21\tder\tder\tDET\tART\tcase=dat|number=sg|gender=fem|prontype=art\t_\t_\t_\t_\n22\tMotion\tMotion\tNOUN\tNN\tcase=dat|number=sg|gender=fem\t_\t_\t_\t_\n23\tPicture\tPicture\tX\tFM\tforeign=foreign\t_\t_\t_\t_\n24\tAssociation\tAssociation\tX\tFM\tforeign=foreign\t_\t_\t_\t_\n25\tof\tof\tX\tFM\tforeign=foreign\t_\t_\t_\t_\n26\tAmerica\tAmerica\tX\tFM\tforeign=foreign\t_\t_\t_\t_\n27\tund\tund\tCONJ\tKON\t_\t_\t_\t_\t_\n28\tder\tder\tDET\tART\tcase=dat|number=sg|gender=fem|prontype=art\t_\t_\t_\t_\n29\tConsumer\tConsumer\tNOUN\tNN\tcase=dat|number=sg|gender=fem\t_\t_\t_\t_\n30\tTechnology\tTechnology\tPROPN\tNE\tcase=nom|number=sg|gender=fem\t_\t_\t_\t_\n31\tAssociation\tAssociation\tPROPN\tNE\tcase=nom|number=sg|gender=fem\t_\t_\t_\t_\n32\teingeladen\teinladen\tVERB\tVVPP\taspect=perf|verbform=part\t_\t_\t_\t_\n33\twaren\tsein\tAUX\tVAFIN\tnumber=pl|person=3|tense=past|mood=ind|verbform=fin\t_\t_\t_\tSpaceAfter=No\n34\t.\t--\tPUNCT\t$.\tpuncttype=peri\t_\t_\t_\t_";
		// Then send to python
		System.out.println("Connecting to Parser...");
		URL url;
		try {
			url = new URL(getRestEndpoint());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-Length", String.valueOf(input.length()));

			System.out.println("Sending to NER Tagger...");
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(input);
			writer.flush();
							
			// Then recieve result and updateCas		
			System.out.println("Waiting on Tagger...");
			String res = IOUtils.toString(connection.getInputStream());
			System.out.println("Recieved from Tagger.");
			//System.out.println(res);
					
			writer.close();
			
			JSONObject result = new JSONObject(res);
			System.out.println(result);

			//System.out.println(result);
			JSONArray sentences = result.getJSONArray("tokens");
			
			Type namedEntityType = JCasUtil.getType(aJCas, NamedEntity.class);
	        Feature namedEntityValue = namedEntityType.getFeatureByBaseName("value");
	        IobDecoder decoder = new IobDecoder(aJCas.getCas(), namedEntityValue, namedEntityMappingProvider);
			
			int sentID = 0;
			for (Sentence sent: JCasUtil.select(aJCas, Sentence.class)) {
				//System.out.println("Processing sentence: " + Integer.toString(sentID + 1));
				JSONArray sentence = sentences.getJSONArray(sentID);
				List<Token> words = JCasUtil.selectCovered(Token.class, sent);
				List<Token> tokens = new ArrayList<Token>();
				String[] namedEntityTags = new String[words.size()];
				System.out.println(sentence.toString());
				int lineID = 0;
				for (Token token : words) {
					List<Object> parsedLine = sentence.getJSONArray(lineID).toList();
					String tag = (String) parsedLine.get(1);
					String parsedToken = (String) parsedLine.get(0);
					System.out.println(parsedLine);
					System.out.println(token.getCoveredText() + " " + parsedToken + " " + tag + " " + Integer.toString(lineID));
					if (token.getCoveredText().equals(parsedToken)) {
						tokens.add(token);
						namedEntityTags[lineID] = tag;
					}
					else {
						System.out.println("Token mismatch between system output and input!");
						throw new AnalysisEngineProcessException();
						//Throw an error of some kind: tokens aren't aligned properly between input and output
					}
					lineID += 1;
				}
				decoder.decode(tokens, namedEntityTags);
				sentID +=1;
			}
		} catch (Exception ex) {
			throw new AnalysisEngineProcessException(ex);
		}
		
				
		
	}

	//Taken from TU-Darmstadt CONLLU Reader Module
    private Dependency makeDependency(JCas aJCas, int govId, int depId, String label,
    		Int2ObjectMap<Token> tokens)
    {
        Dependency rel;
        
        String flavor = DependencyFlavor.BASIC;

        if (govId == 0) {
            rel = new ROOT(aJCas);
            rel.setGovernor(tokens.get(depId));
            rel.setDependent(tokens.get(depId));
        }
        else {
            rel = new Dependency(aJCas);
            rel.setGovernor(tokens.get(govId));
            rel.setDependent(tokens.get(depId));
        }

        rel.setDependencyType(label);
        rel.setFlavor(flavor);
        rel.setBegin(rel.getDependent().getBegin());
        rel.setEnd(rel.getDependent().getEnd());
        rel.addToIndexes();

        return rel;
    }
	
	private String getRestEndpoint() {
		return restEndpointBase + "/" + restEndpointVerb;
	}
}