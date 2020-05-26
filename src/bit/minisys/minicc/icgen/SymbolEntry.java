package bit.minisys.minicc.icgen;

enum SymbolKind{
	SYM_FUNC,
	SYM_VAR,
	SYM_TEMP//¡Ÿ ±±‰¡ø
}

public class SymbolEntry {
	public String name;
	public String type;
	public SymbolKind kind;
	public int tokenId;
	
	private static int totalCount = 0;
    private int entryId;
    
    public SymbolEntry(){
        ++totalCount;
        this.entryId = totalCount;
    }

	public int getEntryId() {
		return entryId;
	}
    
}
