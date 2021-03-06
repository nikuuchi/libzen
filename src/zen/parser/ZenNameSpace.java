// ***************************************************************************
// Copyright (c) 2013-2014, Konoha project authors. All rights reserved.
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
package zen.parser;
import java.util.ArrayList;

import zen.ast.ZenNode;
import zen.deps.LibNative;
import zen.deps.LibZen;
import zen.deps.ZenMap;
import zen.lang.ZenFunc;
import zen.lang.ZenSystem;
import zen.lang.ZenType;
import zen.obsolete.GtFuncBlock;
import zen.obsolete.GtPolyFunc;
//endif VAJA

final class ZenSymbolSource {
	/*field*/public ZenToken SourceToken;
	/*field*/public ZenType  Type; // nullable
	/*field*/public Object  Value;
}

public final class ZenNameSpace extends ZenUtils {
	//	/*field*/public final GtParserContext		Context;
	/*field*/public final ZenNameSpace   ParentNameSpace;
	/*field*/public final ZenGenerator		    Generator;

	/*field*/ZenTokenFunc[] TokenMatrix;
	/*field*/ZenMap<Object>	 SymbolPatternTable;
	/*field*/GtFuncBlock  FuncBlock;

	public ZenNameSpace(ZenGenerator Generator, ZenNameSpace ParentNameSpace) {
		//		this.Context = Context;
		this.ParentNameSpace = ParentNameSpace;
		this.TokenMatrix = null;
		this.SymbolPatternTable = null;
		if(ParentNameSpace == null) {
			this.Generator = Generator;
			this.FuncBlock = null;
			ZenSystem.InitNameSpace(this);
		}
		else {
			this.Generator = ParentNameSpace.Generator;
			this.FuncBlock = ParentNameSpace.FuncBlock;
		}
	}

	public ZenNameSpace CreateSubNameSpace() {
		return new ZenNameSpace(null, this);
	}

	public final ZenNameSpace Minimum() {
		/*local*/ZenNameSpace NameSpace = this;
		while(NameSpace.SymbolPatternTable == null) {
			NameSpace = NameSpace.ParentNameSpace;
		}
		return NameSpace;
	}

	public final ZenNameSpace GetNameSpace(int NameSpaceFlag) {
		if(ZenUtils.IsFlag(NameSpaceFlag, ZenParserConst.PublicNameSpace)) {
			return this.ParentNameSpace;
		}
		return this;
	}

	// TokenMatrix

	public final ZenTokenFunc GetTokenFunc(int GtChar2) {
		if(this.TokenMatrix == null) {
			return this.ParentNameSpace.GetTokenFunc(GtChar2);
		}
		return this.TokenMatrix[GtChar2];
	}

	private final ZenTokenFunc JoinParentFunc(ZenFunc Func, ZenTokenFunc Parent) {
		if(Parent != null && Parent.Func == Func) {
			return Parent;
		}
		return new ZenTokenFunc(Func, Parent);
	}

	public final void AppendTokenFunc(String keys, ZenFunc TokenFunc) {
		/*local*/int i = 0;
		if(this.TokenMatrix == null) {
			this.TokenMatrix = new ZenTokenFunc[ZenParserConst.MaxSizeOfChars];
			if(this.ParentNameSpace != null) {
				while(i < ZenParserConst.MaxSizeOfChars) {
					this.TokenMatrix[i] = this.ParentNameSpace.GetTokenFunc(i);
					i += 1;
				}
			}
		}
		i = 0;
		while(i < keys.length()) {
			/*local*/int kchar = ZenUtils.AsciiToTokenMatrixIndex(LibZen.CharAt(keys, i));
			this.TokenMatrix[kchar] = this.JoinParentFunc(TokenFunc, this.TokenMatrix[kchar]);
			i += 1;
		}
	}

	// SymbolTable
	public final Object GetLocalUndefinedSymbol(String Key) {
		if(this.SymbolPatternTable != null) {
			return this.SymbolPatternTable.GetOrNull(Key);
		}
		return null;
	}

	public final Object GetLocalSymbol(String Key) {
		if(this.SymbolPatternTable != null) {
			/*local*/Object Value = this.SymbolPatternTable.GetOrNull(Key);
			if(Value != null) {
				return Value == ZenParserConst.UndefinedSymbol ? null : Value;
			}
		}
		return null;
	}

	public final Object GetSymbol(String Key) {
		/*local*/ZenNameSpace NameSpace = this;
		while(NameSpace != null) {
			if(NameSpace.SymbolPatternTable != null) {
				/*local*/Object Value = NameSpace.SymbolPatternTable.GetOrNull(Key);
				if(Value != null) {
					return Value == ZenParserConst.UndefinedSymbol ? null : Value;
				}
			}
			NameSpace = NameSpace.ParentNameSpace;
		}
		return null;
	}

	public final boolean HasSymbol(String Key) {
		return (this.GetSymbol(Key) != null);
	}

	public final void SetSymbol(String Key, Object Value, ZenToken SourceToken) {
		if(this.SymbolPatternTable == null) {
			this.SymbolPatternTable = new ZenMap<Object>(null);
		}
		if(SourceToken != null) {
			/*local*/Object OldValue = this.SymbolPatternTable.GetOrNull(Key);
			if(OldValue != null && OldValue != ZenParserConst.UndefinedSymbol) {
				if(LibZen.DebugMode) {
					this.Generator.Logger.ReportWarning(SourceToken, "duplicated symbol: " + SourceToken + " old, new =" + OldValue + ", " + Value);
				}
				else {
					if(!LibZen.EqualsString(Key, "_")) {
						this.Generator.Logger.ReportWarning(SourceToken, "duplicated symbol: " + SourceToken);
					}
				}
			}
		}
		this.SymbolPatternTable.put(Key, Value);
		ZenLogger.VerboseLog(ZenLogger.VerboseSymbol, "symbol: " + Key + ", " + Value);
	}

	public GtVariableInfo SetLocalVariable(int VarFlag, ZenType Type, String Name, ZenToken SourceToken) {
		/*local*/GtVariableInfo VarInfo = new GtVariableInfo(this.FuncBlock, VarFlag, Type, Name, SourceToken);
		this.SetSymbol(Name, VarInfo, SourceToken);
		return VarInfo;
	}

	public final void SetUndefinedSymbol(String Symbol, ZenToken SourceToken) {
		this.SetSymbol(Symbol, ZenParserConst.UndefinedSymbol, SourceToken);
	}

	public final String GetSymbolText(String Key) {
		/*local*/Object Body = this.GetSymbol(Key);
		if(Body instanceof String) {
			return (/*cast*/String)Body;
		}
		return null;
	}

	public final ZenType GetSymbolType(String Symbol) {
		return ZenSystem.VarType;
	}

	// Pattern
	public ZenSyntaxPattern GetSyntaxPattern(String PatternName) {
		/*local*/Object Body = this.GetSymbol(PatternName);
		if(Body instanceof ZenSyntaxPattern) {
			return (/*cast*/ZenSyntaxPattern)Body;
		}
		return null;
	}

	public ZenSyntaxPattern GetExtendedSyntaxPattern(String PatternName) {
		/*local*/Object Body = this.GetSymbol(ZenNameSpace.SuffixPatternSymbol(PatternName));
		if(Body instanceof ZenSyntaxPattern) {
			return (/*cast*/ZenSyntaxPattern)Body;
		}
		return null;
	}

	private void AppendSyntaxPattern(String PatternName, ZenSyntaxPattern NewPattern, ZenToken SourceToken) {
		LibNative.Assert(NewPattern.ParentPattern == null);
		/*local*/ZenSyntaxPattern ParentPattern = this.GetSyntaxPattern(PatternName);
		NewPattern.ParentPattern = ParentPattern;
		this.SetSymbol(PatternName, NewPattern, SourceToken);
	}

	public void AppendSyntax(String PatternName, ZenFunc MatchFunc) {
		/*local*/int Alias = PatternName.indexOf(" ");
		/*local*/String Name = (Alias == -1) ? PatternName : PatternName.substring(0, Alias);
		/*local*/ZenSyntaxPattern Pattern = new ZenSyntaxPattern(this, Name, MatchFunc);
		this.AppendSyntaxPattern(Name, Pattern, null);
		if(Alias != -1) {
			this.AppendSyntax(PatternName.substring(Alias+1), MatchFunc);
		}
	}

	public void AppendSuffixSyntax(String PatternName, int SyntaxFlag, ZenFunc MatchFunc) {
		/*local*/int Alias = PatternName.indexOf(" ");
		/*local*/String Name = (Alias == -1) ? PatternName : PatternName.substring(0, Alias);
		/*local*/ZenSyntaxPattern Pattern = new ZenSyntaxPattern(this, Name, MatchFunc);
		Pattern.SyntaxFlag = SyntaxFlag;
		this.AppendSyntaxPattern(ZenNameSpace.SuffixPatternSymbol(Name), Pattern, null);
		if(Alias != -1) {
			this.AppendSuffixSyntax(PatternName.substring(Alias+1), SyntaxFlag, MatchFunc);
		}
	}



	public final ZenType GetType(String TypeName) {
		/*local*/Object TypeInfo = this.GetSymbol(TypeName);
		if(TypeInfo instanceof ZenType) {
			return (/*cast*/ZenType)TypeInfo;
		}
		return null;
	}

	public final ZenType AppendTypeName(ZenType Type, ZenToken SourceToken) {
		if(Type.GetBaseType() == Type) {
			this.SetSymbol(Type.ShortName, Type, SourceToken);
		}
		return Type;
	}

	//	public final ZenType AppendTypeVariable(String Name, ZenType ParamBaseType, GtToken SourceToken, ArrayList<Object> RevertList) {
	//		this.UpdateRevertList(Name, RevertList);
	//		/*local*/ZenType TypeVar = new ZenType(TypeVariable, Name, ParamBaseType, null);
	//		this.SetSymbol(Name, TypeVar, SourceToken);
	//		return TypeVar;
	//	}

	public final Object GetClassSymbol(ZenType ClassType, String Symbol, boolean RecursiveSearch) {
		while(ClassType != null) {
			/*local*/String Key = ZenNameSpace.ClassSymbol(ClassType, Symbol);
			/*local*/Object Value = this.GetSymbol(Key);
			if(Value != null) {
				return Value;
			}
			//			if(ClassType.IsDynamicNaitiveLoading() & this.Context.RootNameSpace.GetLocalUndefinedSymbol(Key) == null) {
			//				Value = LibZen.LoadNativeStaticFieldValue(ClassType, Symbol.substring(1));
			//				if(Value != null) {
			//					return Value;
			//				}
			//				//LibZen.LoadNativeMethods(ClassType, Symbol, FuncList);
			//			}
			if(!RecursiveSearch) {
				break;
			}
			ClassType = ClassType.GetSuperType();
		}
		return null;
	}

	//	public final Object GetClassStaticSymbol(GtType StaticClassType, String Symbol, boolean RecursiveSearch) {
	//		/*local*/String Key = null;
	//		/*local*/GtType ClassType = StaticClassType;
	//		while(ClassType != null) {
	//			Key = GtNameSpace.ClassStaticSymbol(ClassType, Symbol);
	//			/*local*/Object Value = this.GetSymbol(Key);
	//			if(Value != null) {
	//				return Value;
	//			}
	//			if(!RecursiveSearch) {
	//				break;
	//			}
	//			ClassType = ClassType.SuperType;
	//		}
	//		Key = GtNameSpace.ClassStaticSymbol(StaticClassType, Symbol);
	//		if(StaticClassType.IsDynamicNaitiveLoading() && this.Context.RootNameSpace.GetLocalUndefinedSymbol(Key) == null) {
	//			/*local*/Object Value = LibNative.ImportStaticFieldValue(this.Context, StaticClassType, Symbol);
	//			if(Value == null) {
	//				this.Context.RootNameSpace.SetUndefinedSymbol(Key, null);
	//			}
	//			else {
	//				this.Context.RootNameSpace.SetSymbol(Key, Value, null);
	//			}
	//			return Value;
	//		}
	//		return null;
	//	}

	//	public final void ImportClassSymbol(GtNameSpace NameSpace, String Prefix, GtType ClassType, GtToken SourceToken) {
	//		/*local*/String ClassPrefix = ClassSymbol(ClassType, ClassStaticName(""));
	//		/*local*/ArrayList<String> KeyList = new ArrayList<String>();
	//		/*local*/GtNameSpace ns = NameSpace;
	//		while(ns != null) {
	//			if(ns.SymbolPatternTable != null) {
	//				LibZen.RetrieveMapKeys(ns.SymbolPatternTable, ClassPrefix, KeyList);
	//			}
	//			ns = ns.ParentNameSpace;
	//		}
	//		/*local*/int i = 0;
	//		while(i < KeyList.size()) {
	//			/*local*/String Key = KeyList.get(i);
	//			/*local*/Object Value = NameSpace.GetSymbol(Key);
	//			Key = Key.replace(ClassPrefix, Prefix);
	//			if(SourceToken != null) {
	//				SourceToken.ParsedText = Key;
	//			}
	//			this.SetSymbol(Key, Value, SourceToken);
	//			i = i + 1;
	//		}
	//	}

	//	public final GtFunc GetGetterFunc(GtType ClassType, String Symbol, boolean RecursiveSearch) {
	//		/*local*/Object Func = this.Context.RootNameSpace.GetClassSymbol(ClassType, GtNameSpace.GetterSymbol(Symbol), RecursiveSearch);
	//		if(Func instanceof GtFunc) {
	//			return (/*cast*/GtFunc)Func;
	//		}
	//		Func = this.Context.RootNameSpace.GetLocalUndefinedSymbol(GtNameSpace.ClassSymbol(ClassType, GtNameSpace.GetterSymbol(Symbol)));
	//		if(ClassType.IsDynamicNaitiveLoading() && Func == null) {
	//			return LibZen.LoadNativeField(this.Context, ClassType, Symbol, false);
	//		}
	//		return null;
	//	}
	//
	//	public final GtFunc GetSetterFunc(GtType ClassType, String Symbol, boolean RecursiveSearch) {
	//		/*local*/Object Func = this.Context.RootNameSpace.GetClassSymbol(ClassType, GtNameSpace.SetterSymbol(Symbol), RecursiveSearch);
	//		if(Func instanceof GtFunc) {
	//			return (/*cast*/GtFunc)Func;
	//		}
	//		Func = this.Context.RootNameSpace.GetLocalUndefinedSymbol(GtNameSpace.ClassSymbol(ClassType, GtNameSpace.SetterSymbol(Symbol)));
	//		if(ClassType.IsDynamicNaitiveLoading() && Func == null) {
	//			return LibZen.LoadNativeField(this.Context, ClassType, Symbol, true);
	//		}
	//		return null;
	//	}
	//
	//	public final GtFunc GetConverterFunc(GtType FromType, GtType ToType, boolean RecursiveSearch) {
	//		/*local*/Object Func = this.GetClassSymbol(FromType, GtNameSpace.ConverterSymbol(ToType), RecursiveSearch);
	//		if(Func instanceof GtFunc) {
	//			return (/*cast*/GtFunc)Func;
	//		}
	//		return null;
	//	}
	//
	//	public final GtPolyFunc GetMethod(GtType ClassType, String Symbol, boolean RecursiveSearch) {
	//		/*local*/ArrayList<GtFunc> FuncList = new ArrayList<GtFunc>();
	//		while(ClassType != null) {
	//			/*local*/String Key = GtNameSpace.ClassSymbol(ClassType, Symbol);
	//			/*local*/Object RootValue = this.RetrieveFuncList(Key, FuncList);
	//			if(RootValue == null && ClassType.IsDynamicNaitiveLoading()) {
	//				if(LibZen.EqualsString(Symbol, GtNameSpace.ConstructorSymbol())) {
	//					LibZen.LoadNativeConstructors(this.Context, ClassType, FuncList);
	//				}
	//				else {
	//					LibZen.LoadNativeMethods(this.Context, ClassType, Symbol, FuncList);
	//				}
	//			}
	//			if(!RecursiveSearch) {
	//				break;
	//			}
	//			//System.err.println("** " + ClassType + ", " + ClassType.ParentMethodSearch);
	//			ClassType = ClassType.ParentMethodSearch;
	//		}
	//		return new GtPolyFunc(FuncList);
	//	}
	//
	//	public final GtPolyFunc GetConstructorFunc(GtType ClassType) {
	//		return this.Context.RootNameSpace.GetMethod(ClassType, GtNameSpace.ConstructorSymbol(), false);
	//	}
	//
	//	public final GtFunc GetOverridedMethod(GtType ClassType, GtFunc GivenFunc) {
	//		/*local*/String Symbol = GtNameSpace.FuncSymbol(GivenFunc.FuncName);
	//		/*local*/GtType GivenClassType = GivenFunc.GetRecvType();
	//		if(ClassType != GivenClassType) {
	//			/*local*/ArrayList<GtFunc> FuncList = new ArrayList<GtFunc>();
	//			while(ClassType != null) {
	//				/*local*/String Key = GtNameSpace.ClassSymbol(ClassType, Symbol);
	//				this.RetrieveFuncList(Key, FuncList);
	//				/*local*/int i = 0;
	//				while(i < FuncList.size()) {
	//					/*local*/GtFunc Func = FuncList.get(i);
	//					i += 1;
	//					if(Func.EqualsOverridedMethod(GivenFunc)) {
	//						return Func;
	//					}
	//				}
	//				FuncList.clear();
	//				ClassType = ClassType.ParentMethodSearch;
	//			}
	//		}
	//		return GivenFunc;
	//	}

	public final Object RetrieveFuncList(String FuncName, ArrayList<ZenFunc> FuncList) {
		/*local*/Object FuncValue = this.GetLocalSymbol(FuncName);
		if(FuncValue instanceof ZenFunc) {
			/*local*/ZenFunc Func = (/*cast*/ZenFunc)FuncValue;
			FuncList.add(Func);
		}
		else if(FuncValue instanceof GtPolyFunc) {
			/*local*/GtPolyFunc PolyFunc = (/*cast*/GtPolyFunc)FuncValue;
			/*local*/int i = PolyFunc.FuncList.size() - 1;
			while(i >= 0) {
				FuncList.add(PolyFunc.FuncList.get(i));
				i = i - 1;
			}
		}
		if(this.ParentNameSpace != null) {
			return this.ParentNameSpace.RetrieveFuncList(FuncName, FuncList);
		}
		return FuncValue;
	}

	public final GtPolyFunc GetPolyFunc(String FuncName) {
		/*local*/ArrayList<ZenFunc> FuncList = new ArrayList<ZenFunc>();
		this.RetrieveFuncList(FuncName, FuncList);
		return new GtPolyFunc(null, FuncList);
	}

	public final ZenFunc GetFunc(String FuncName, int BaseIndex, ArrayList<ZenType> TypeList) {
		/*local*/ArrayList<ZenFunc> FuncList = new ArrayList<ZenFunc>();
		this.RetrieveFuncList(FuncName, FuncList);
		/*local*/int i = 0;
		while(i < FuncList.size()) {
			/*local*/ZenFunc Func = FuncList.get(i);
			if(Func.Types.length == TypeList.size() - BaseIndex) {
				/*local*/int j = 0;
				while(j < Func.Types.length) {
					if(TypeList.get(BaseIndex + j) != Func.Types[j]) {
						Func = null;
						break;
					}
					j = j + 1;
				}
				if(Func != null) {
					return Func;
				}
			}
			i = i + 1;
		}
		return null;
	}

	//	public final Object AppendFuncName(String Key, GtFunc Func, GtToken SourceToken) {
	//		/*local*/Object OldValue = this.GetLocalSymbol(Key);
	//		if(OldValue instanceof GtSyntaxPattern) {
	//			return OldValue;
	//		}
	//		if(OldValue instanceof GtFunc) {
	//			/*local*/GtFunc OldFunc = (/*cast*/GtFunc)OldValue;
	//			if(!OldFunc.EqualsType(Func)) {
	//				/*local*/GtPolyFunc PolyFunc = new GtPolyFunc(null);
	//				PolyFunc.Append(this.Context, OldFunc, SourceToken);
	//				PolyFunc.Append(this.Context, Func, SourceToken);
	//				this.SetSymbol(Key, PolyFunc, null);
	//				return PolyFunc;
	//			}
	//			// error
	//		}
	//		else if(OldValue instanceof GtPolyFunc) {
	//			/*local*/GtPolyFunc PolyFunc = (/*cast*/GtPolyFunc)OldValue;
	//			PolyFunc.Append(this.Context, Func, SourceToken);
	//			return PolyFunc;
	//		}
	//		this.SetSymbol(Key, Func, SourceToken);
	//		return OldValue;
	//	}
	//
	//	public final Object AppendFunc(GtFunc Func, GtToken SourceToken) {
	//		return this.AppendFuncName(Func.FuncName, Func, SourceToken);
	//	}
	//
	//	public final Object AppendStaticFunc(GtType StaticType, GtFunc Func, GtToken SourceToken) {
	//		/*local*/int loc = Func.FuncName.lastIndexOf(".");
	//		return this.AppendFuncName(GtNameSpace.ClassStaticSymbol(StaticType, Func.FuncName.substring(loc+1)), Func, SourceToken);
	//	}
	//
	//	public final Object AppendMethod(GtFunc Func, GtToken SourceToken) {
	//		/*local*/GtType ClassType = Func.GetRecvType();
	//		if(ClassType.IsGenericType() && ClassType.HasTypeVariable()) {
	//			ClassType = ClassType.BaseType;
	//		}
	//		/*local*/String Key = GtNameSpace.ClassSymbol(ClassType, Func.FuncName);
	//		return this.AppendFuncName(Key, Func, SourceToken);
	//	}
	//
	//	public final void AppendConstructor(GtType ClassType, GtFunc Func, GtToken SourceToken) {
	//		/*local*/String Key = GtNameSpace.ClassSymbol(ClassType, GtNameSpace.ConstructorSymbol());
	//		LibNative.Assert(Func.Is(ConstructorFunc));
	//		this.Context.RootNameSpace.AppendFuncName(Key, Func, SourceToken);  // @Public
	//	}
	//
	//	public final void SetGetterFunc(GtType ClassType, String Name, GtFunc Func, GtToken SourceToken) {
	//		/*local*/String Key = GtNameSpace.ClassSymbol(ClassType, GtNameSpace.GetterSymbol(Name));
	//		LibNative.Assert(Func.Is(GetterFunc));
	//		this.Context.RootNameSpace.SetSymbol(Key, Func, SourceToken);  // @Public
	//	}
	//
	//	public final void SetSetterFunc(GtType ClassType, String Name, GtFunc Func, GtToken SourceToken) {
	//		/*local*/String Key = GtNameSpace.ClassSymbol(ClassType, GtNameSpace.SetterSymbol(Name));
	//		LibNative.Assert(Func.Is(SetterFunc));
	//		this.Context.RootNameSpace.SetSymbol(Key, Func, SourceToken);  // @Public
	//	}
	//
	//	public final void SetConverterFunc(GtType ClassType, GtType ToType, GtFunc Func, GtToken SourceToken) {
	//		if(ClassType == null) {
	//			ClassType = Func.GetFuncParamType(0);
	//		}
	//		if(ToType == null) {
	//			ToType = Func.GetReturnType();
	//		}
	//		/*local*/String Key = GtNameSpace.ClassSymbol(ClassType, GtNameSpace.ConverterSymbol(ToType));
	//		LibNative.Assert(Func.Is(ConverterFunc));
	//		this.SetSymbol(Key, Func, SourceToken);
	//	}

	final Object EvalWithErrorInfo(String ScriptText, long FileLine) {
		/*local*/Object ResultValue = null;
		ZenLogger.VerboseLog(ZenLogger.VerboseEval, "eval: " + ScriptText);
		/*local*/ZenTokenContext TokenContext = new ZenTokenContext(this, ScriptText, FileLine);
		TokenContext.SkipEmptyStatement();
		while(TokenContext.HasNext()) {
			TokenContext.ParseFlag = 0; // init
			TokenContext.SkipAndGetAnnotation(true);
			/*local*/ZenNode TopLevelNode = TokenContext.ParsePattern(this, "$Statement$", ZenParserConst.Required);
			//			TopLevelNode = this.TypeCheck(TopLevelNode, GtStaticTable.VoidType, GreenTeaConsts.AllowVoidPolicy);
			this.Generator.DoCodeGeneration(this, TopLevelNode);
			//			TopLevelNode.Accept(this.Generator);
			if(TopLevelNode.IsErrorNode() && TokenContext.HasNext()) {
				/*local*/ZenToken Token = TokenContext.GetToken();
				this.Generator.Logger.ReportInfo(Token, "stopped script at this line");
				return null;
			}
			//			if(!TopLevelNode.Type.IsVoidType()) {
			ResultValue = this.Generator.EvalTopLevelNode(TopLevelNode);
			//			}
			TokenContext.SkipEmptyStatement();
			TokenContext.Vacume();
		}
		return ResultValue;
	}

	public final Object Eval(String ScriptText, long FileLine) {
		/*local*/Object ResultValue = this.EvalWithErrorInfo(ScriptText, FileLine);
		if(ResultValue instanceof ZenToken && ((/*cast*/ZenToken)ResultValue).IsError()) {
			return null;
		}
		return ResultValue;
	}

	public final boolean Load(String ScriptText, long FileLine) {
		/*local*/Object Token = this.EvalWithErrorInfo(ScriptText, FileLine);
		if(Token instanceof ZenToken && ((/*cast*/ZenToken)Token).IsError()) {
			return false;
		}
		return true;
	}

	public final boolean LoadFile(String FileName) {
		/*local*/String ScriptText = LibNative.LoadScript(FileName);
		if(ScriptText != null) {
			/*local*/long FileLine = ZenSystem.GetFileLine(FileName, 1);
			return this.Load(ScriptText, FileLine);
		}
		return false;
	}

	public final boolean LoadRequiredLib(String LibName) {
		/*local*/String Key = ZenParserConst.NativeNameSuffix + "L" + LibName.toLowerCase();
		if(!this.HasSymbol(Key)) {
			/*local*/String Path = LibZen.GetLibPath(this.Generator.TargetCode, LibName);
			/*local*/String Script = LibNative.LoadScript(Path);
			if(Script != null) {
				/*local*/long FileLine = ZenSystem.GetFileLine(Path, 1);
				if(this.Load(Script, FileLine)) {
					this.SetSymbol(Key, Path, null);
					return true;
				}
			}
			return false;
		}
		return true;
	}

	private void UpdateRevertList(String Key, ArrayList<Object> RevertList) {
		/*local*/Object Value = this.GetLocalSymbol(Key);
		RevertList.add(Key);
		if(Value != null) {
			RevertList.add(Value);
		}
		else {
			RevertList.add(ZenParserConst.UndefinedSymbol);
		}
	}

	public void Revert(ArrayList<Object> RevertList) {
		/*local*/int i = 0;
		while(i < RevertList.size()) {
			/*local*/String Key = (/*cast*/String)RevertList.get(i);
			/*local*/Object Value = RevertList.get(i+1);
			this.SetSymbol(Key, Value, null);
			i += 2;
		}
	}

	public final static String FuncSymbol(String Symbol) {
		return LibZen.IsVariableName(Symbol, 0) ? Symbol : "__" + Symbol;
	}

	public final static String ConverterSymbol(ZenType ClassType) {
		return ClassType.GetUniqueName();
	}

	public final static String ConstructorSymbol() {
		return "";
	}

	public final static String SetterSymbol(String Symbol) {
		return Symbol + "=";
	}

	public final static String GetterSymbol(String Symbol) {
		return Symbol + "+";
	}

	public final static String ClassSymbol(ZenType ClassType, String Symbol) {
		return ClassType.GetUniqueName() + "." + Symbol;
	}

	public final static String ClassStaticSymbol(ZenType ClassType, String Symbol) {
		return ClassType.GetUniqueName() + ".@" + Symbol;
	}

	public final static String SuffixPatternSymbol(String PatternName) {
		return "\t" + PatternName;
	}


}
