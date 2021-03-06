// ***************************************************************************
// Copyright (c) 2013, JST/CREST DEOS project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// *  Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
// *  Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// **************************************************************************

//ifdef JAVA
package zen.codegen.jython;

import zen.ast.GtBlockNode;
import zen.ast.GtCastNode;
import zen.ast.GtCatchNode;
import zen.ast.GtFuncDeclNode;
import zen.ast.GtFunctionLiteralNode;
import zen.ast.GtInstanceOfNode;
import zen.ast.GtParamNode;
import zen.ast.GtReturnNode;
import zen.ast.GtThrowNode;
import zen.ast.GtTryNode;
import zen.ast.GtVarDeclNode;
import zen.ast.ZenNode;
import zen.lang.ZenSystem;
import zen.parser.ZenSourceGenerator;
//endif VAJA

//GreenTea Generator should be written in each language.

public class PythonSourceGenerator extends ZenSourceGenerator {

	public PythonSourceGenerator/* constructor */() {
		super("python", "2.0");
		this.LineFeed = "\n";
		this.Tab = "\t";
		this.LineComment = "#"; // if not, set null
		this.BeginComment = null; //"'''";
		this.EndComment = null; //"'''";
		this.Camma = ", ";
		this.SemiColon = "";

		this.TrueLiteral = "True";
		this.FalseLiteral = "False";
		this.NullLiteral = "None";
		this.TopType = "object";
		this.SetNativeType(ZenSystem.BooleanType, "bool");
		this.SetNativeType(ZenSystem.IntType, "int");
		this.SetNativeType(ZenSystem.FloatType, "float");
		this.SetNativeType(ZenSystem.StringType, "str");
	}

	@Override
	public void VisitBlockNode(GtBlockNode Node) {
		int count = 0;
		this.CurrentBuilder.Append(":");
		this.CurrentBuilder.Indent();
		for (int i = 0; i < Node.StatementList.size(); i++) {
			ZenNode SubNode = Node.StatementList.get(i);
			this.CurrentBuilder.AppendLineFeed();
			this.CurrentBuilder.AppendIndent();
			this.GenerateCode(SubNode);
			this.CurrentBuilder.Append(this.SemiColon);
			count = count + 1;
		}
		if (count == 0) {
			this.CurrentBuilder.AppendLineFeed();
			this.CurrentBuilder.AppendIndent();
			this.CurrentBuilder.Append("pass");
		}
		this.CurrentBuilder.UnIndent();
		this.CurrentBuilder.AppendLineFeed();
		this.CurrentBuilder.AppendIndent();
		this.CurrentBuilder.Append("#");
	}

	@Override public void VisitCastNode(GtCastNode Node) {
		// this.CurrentBuilder.Append("(");
		// this.VisitType(Node.Type);
		// this.CurrentBuilder.Append(") ");
		this.CurrentBuilder.AppendBlockComment("as " + this.GetNativeType(Node.Type));
		this.GenerateCode(Node.ExprNode);
	}

	@Override public void VisitInstanceOfNode(GtInstanceOfNode Node) {
		this.CurrentBuilder.Append("isinstance(");
		this.GenerateCode(Node.LeftNode);
		this.CurrentBuilder.Append(this.Camma);
		this.VisitType(Node.RightNode.Type);
		this.CurrentBuilder.Append(")");
	}

	@Override
	public void VisitThrowNode(GtThrowNode Node) {
		this.CurrentBuilder.Append("raise ");
		this.GenerateCode(Node.ValueNode);
	}

	@Override
	public void VisitTryNode(GtTryNode Node) {
		this.CurrentBuilder.Append("try");
		this.GenerateCode(Node.TryNode);
		for (ZenNode CatchNode : Node.CatchList) {
			this.GenerateCode(CatchNode);
		}
		if (Node.FinallyNode != null) {
			this.CurrentBuilder.Append("finally");
			this.GenerateCode(Node.FinallyNode);
		}
	}

	@Override
	public void VisitCatchNode(GtCatchNode Node) {
		this.CurrentBuilder.Append("except ");
		this.VisitType(Node.ExceptionType);
		this.CurrentBuilder.AppendToken("as");
		this.CurrentBuilder.Append(Node.ExceptionName);
		this.GenerateCode(Node.BodyNode);
	}

	@Override
	public void VisitVarDeclNode(GtVarDeclNode Node) {
		this.CurrentBuilder.Append(Node.NativeName);
		this.CurrentBuilder.AppendToken("=");
		this.GenerateCode(Node.InitNode);
	}

	@Override
	public void VisitParamNode(GtParamNode Node) {
		this.CurrentBuilder.Append(Node.Name);
	}

	/**
	>>> def f(x):
		...   def g(y):
		...     return x + y
		...   return g
		...
		>>> f(1)(3)
		4
	 **/
	@Override
	public void VisitFunctionLiteralNode(GtFunctionLiteralNode Node) {
		GtReturnNode ReturnNode = Node.BodyNode.ToReturnNode();
		if(ReturnNode != null && ReturnNode.ValueNode != null) {
			this.CurrentBuilder.Append("lambda");
			this.VisitParamList(" ", Node.ArgumentList, ": ");
			this.GenerateCode(ReturnNode.ValueNode);
		}
		else {
			this.CurrentBuilder.Append("def");
			this.CurrentBuilder.AppendToken("lambda");
			this.VisitParamList("(", Node.ArgumentList, ")");
			this.GenerateCode(Node.BodyNode);
		}
	}

	@Override
	public void VisitFuncDeclNode(GtFuncDeclNode Node) {
		this.CurrentBuilder.Append("def ");
		this.CurrentBuilder.Append(Node.FuncName);
		this.VisitParamList("(", Node.ArgumentList, ")");
		if (Node.BodyNode == null) {
			this.CurrentBuilder.Append(": pass");
		} else {
			this.GenerateCode(Node.BodyNode);
		}
	}

}