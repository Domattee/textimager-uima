package org.hucompute.textimager.uima.SDP;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

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

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

@TypeCapability(
		inputs = {
				"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
		"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
		"de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
		"de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
		"de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures"},
		outputs = {"de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency"})

public class SDPBase extends JCasAnnotator_ImplBase {
	
    /**
     * The docker image for the dependency parser server
     */
    public static final String PARAM_DOCKER_IMAGE = "dockerImage";
    @ConfigurationParameter(name = PARAM_DOCKER_IMAGE, mandatory = true, defaultValue = "texttechnologylab/textimager-sdp:1")
    protected String dockerImage;
    
    /**
     * The min port
     */
    public static final String PARAM_PORT_MIN = "portMin";
    @ConfigurationParameter(name = PARAM_PORT_MIN, mandatory = true, defaultValue = "5000")
    protected int portMin;
    
    /**
     * The max port
     */
    public static final String PARAM_PORT_MAX = "portMax";
    @ConfigurationParameter(name = PARAM_PORT_MAX, mandatory = true, defaultValue = "5100")
    protected int portMax;
    
    
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		
	}
	
	// IP and Port of the server running the flask parser client
	private String restEndpointBase = "http://127.0.0.1:5000";
	// Request to run parser function
	private String restEndpointVerb = "sdp";
	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		
		System.out.println("Building Input String...");
		// Build the conllu format string that gets send to the parser. Cas has to contain: Tokens, Lemma, xpos and morphs.
		StringBuilder builder = new StringBuilder();
		builder.append("{ \"dict\" : \"");
		for (Sentence sentence: JCasUtil.select(aJCas, Sentence.class)) {
			int ID = 0;
			for (Token token : JCasUtil.selectCovered(Token.class, sentence)) {
				ID += 1;
				String plain_token = token.getCoveredText();
				String lemma = token.getLemma().getValue();
				
				MorphologicalFeatures feats = token.getMorph();
				// TODO: What if Value field is empty, only individual feature tags? how to test?
				String morphString = feats.getValue();
				String xpos = token.getPos().getPosValue();
				String upos = "_";
				builder.append(Integer.toString(ID) + "\\t" + plain_token + "\\t" + lemma + "\\t" + upos + "\\t" + xpos + "\\t" + morphString + "\\t_" + "\\t_" + "\\t_" + "\\t_" + "\\n");
				
			//Now arrange them all in the proper format somehow. Manage sentence boundaries too.
			}
			builder.append("\\n");
		}
		builder.append("\"}");
		
		String input = builder.toString();
		//System.out.println(input);
		
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

			System.out.println("Sending to Parser...");
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			writer.write(input);
			writer.flush();
							
			// Then recieve result and updateCas		
			System.out.println("Waiting on Parser...");
			String res = IOUtils.toString(connection.getInputStream());
			System.out.println("Recieved from Parser");
			//System.out.println(res);
					
			writer.close();
			
			JSONObject result = new JSONObject(res);
			//System.out.println(result);
			JSONArray sentences = result.getJSONArray("sentences");
			int sentID = 0;
			// Read tokens and align with JCas first:
			for (Sentence sent: JCasUtil.select(aJCas, Sentence.class)) {
				//System.out.println("Processing sentence: " + Integer.toString(sentID + 1));
				JSONArray parsedSentence = sentences.getJSONArray(sentID);
				Int2ObjectMap<Token> tokens = new Int2ObjectOpenHashMap<>();
				int lineID = 0;
				for (Token token : JCasUtil.selectCovered(Token.class, sent)) {
					JSONObject parsedLine = parsedSentence.getJSONObject(lineID);
					//System.out.println(parsedLine);
					//System.out.println(token);
					int dependent = parsedLine.getInt("ID");
					String parsedToken = parsedLine.getString("TOKEN");
					lineID += 1;
					if (token.getCoveredText().equals(parsedToken)) {
						tokens.put(dependent, token);
					}
					else {
						throw new AnalysisEngineProcessException();
						//Throw an error of some kind: tokens aren't aligned properly between input and output
					}
				}
				lineID = 0;
				//System.out.println("Processing dependencies for sentence: " + Integer.toString(sentID));
				//System.out.println(tokens);
				for (Token token : JCasUtil.selectCovered(Token.class, sent)) {
					JSONObject parsedLine = parsedSentence.getJSONObject(lineID);
					//System.out.println(parsedLine);
					//System.out.println(token);
					int head = parsedLine.getInt("HEAD");
					int dependent = parsedLine.getInt("ID");
					String label = parsedLine.getString("REL");
					String parsedToken = parsedLine.getString("TOKEN");
					lineID += 1;
					if (token.getCoveredText().equals(parsedToken)) {
						//System.out.println("Making dependency...");
						makeDependency(aJCas, head, dependent, label, tokens);
					}
					else {
						throw new AnalysisEngineProcessException();
						//Throw an error of some kind: tokens aren't aligned properly between input and output
					}
				}
				sentID += 1;
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