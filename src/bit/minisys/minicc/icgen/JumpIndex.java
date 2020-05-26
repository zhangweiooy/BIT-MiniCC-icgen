package bit.minisys.minicc.icgen;

import bit.minisys.minicc.parser.ast.ASTNode;
import bit.minisys.minicc.parser.ast.ASTVisitor;

public class JumpIndex extends ASTNode{
	
	private Integer destIndex;

	public JumpIndex(String type) {
		super(type);
		// TODO Auto-generated constructor stub
	}
	
	public JumpIndex(Integer id) {
		super("JumpIndex");
		this.destIndex = id;
	}

	@Override
	public void accept(ASTVisitor visitor) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public Integer getDestIndex() {
		return destIndex;
	}

	public void setDestIndex(Integer destIndex) {
		this.destIndex = destIndex;
	}


}
