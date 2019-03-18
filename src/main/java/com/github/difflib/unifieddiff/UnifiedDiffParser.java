import com.github.difflib.patch.ChangeDelta;
import com.github.difflib.patch.Chunk;
import java.util.ArrayList;
import java.util.List;
    static final Pattern UNIFIED_DIFF_CHUNK_REGEXP = Pattern.compile("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@");

    private final UnifiedDiffLine[] MAIN_PARSER_RULES = new UnifiedDiffLine[]{
        new UnifiedDiffLine(true, "^index\\s[\\da-zA-Z]+\\.\\.[\\da-zA-Z]+(\\s(\\d+))?$", this::processIndex),
        new UnifiedDiffLine(true, "^---\\s", this::processFromFile),
        new UnifiedDiffLine(true, "^\\+\\+\\+\\s", this::processToFile),
        new UnifiedDiffLine(false, UNIFIED_DIFF_CHUNK_REGEXP, this::processChunk),
        new UnifiedDiffLine("^\\s+", this::processNormalLine),
        new UnifiedDiffLine("^-", this::processDelLine),
        new UnifiedDiffLine("^+", this::processAddLine)

    private UnifiedDiff parse() throws IOException, UnifiedDiffParserException {
        String tailTxt = "";
            if (line.matches("^\\-\\-\\s+")) {
                break;
            } else {
                LOG.log(Level.INFO, "parsing line {0}", line);
                if (processLine(header, line) == false) {
                    if (header) {
                        headerTxt += line + "\n";
                    } else {
                        break;
                    }
                    header = false;
                    data.setHeader(headerTxt);

        finalizeChunk();

        while (READER.ready()) {
            tailTxt += READER.readLine() + "\n";
        }
        data.setTailTxt(tailTxt);

    public static UnifiedDiff parseUnifiedDiff(InputStream stream) throws IOException, UnifiedDiffParserException {
    private boolean processLine(boolean header, String line) throws UnifiedDiffParserException {
        for (UnifiedDiffLine rule : MAIN_PARSER_RULES) {
        if (!originalTxt.isEmpty() || !revisedTxt.isEmpty()) {
            finalizeChunk();
            actualFile = null;
        }
            data.addFile(actualFile);
        actualFile.setDiffCommand(line);
    }

    private List<String> originalTxt = new ArrayList<>();
    private List<String> revisedTxt = new ArrayList<>();
    private int old_ln;
    private int new_ln;

    private void finalizeChunk() {
        if (!originalTxt.isEmpty() || !revisedTxt.isEmpty()) {
            actualFile.getPatch().addDelta(new ChangeDelta<>(new Chunk<>(
                    old_ln - 1, originalTxt), new Chunk<>(
                    new_ln - 1, revisedTxt)));
            old_ln = 0;
            new_ln = 0;
            originalTxt.clear();
            revisedTxt.clear();
        }
    }

    public void processNormalLine(MatchResult match, String line) {
        String cline = line.substring(1);
        originalTxt.add(cline);
        revisedTxt.add(cline);
    }

    public void processAddLine(MatchResult match, String line) {
        String cline = line.substring(1);
        revisedTxt.add(cline);
    }

    public void processDelLine(MatchResult match, String line) {
        String cline = line.substring(1);
        originalTxt.add(cline);
    }

    public void processChunk(MatchResult match, String chunkStart) {
        finalizeChunk();
        old_ln = match.group(1) == null ? 1 : Integer.parseInt(match.group(1));
        new_ln = match.group(3) == null ? 1 : Integer.parseInt(match.group(3));
        if (old_ln == 0) {
            old_ln = 1;
        }
        if (new_ln == 0) {
            new_ln = 1;
        }
    private void processFromFile(MatchResult match, String line) {
        initFileIfNecessary();
        actualFile.setFromFile(extractFileName(line));
    }

    private void processToFile(MatchResult match, String line) {
        initFileIfNecessary();
        actualFile.setToFile(extractFileName(line));
    }

    private String extractFileName(String line) {
        return line.substring(4).replaceFirst("^(a|b)\\/", "");
    }

        public UnifiedDiffLine(boolean stopsHeaderParsing, Pattern pattern, BiConsumer<MatchResult, String> command) {
            this.pattern = pattern;
            this.command = command;
            this.stopsHeaderParsing = stopsHeaderParsing;
        }

        public boolean processLine(String line) throws UnifiedDiffParserException {