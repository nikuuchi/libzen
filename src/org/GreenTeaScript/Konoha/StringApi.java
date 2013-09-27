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

package org.GreenTeaScript.Konoha;

import org.GreenTeaScript.GtType;

public final class StringApi {
	public static boolean Equals(String x, String y) {
		if(x == null || y == null) {
			return x == y;
		}
		else {
			return x.equals(y);
		}
	}
	public static boolean NotEquals(String x, String y) {
		return !Equals(x, y);
	}
	public static String Add(String x, Object y) {
		return x + y;
	}
//	public static String Add(long x, String y) {
//		return x + y;
//	}
//
//	public static String Add(boolean x, String y) {
//		return x + y;
//	}
	public final static long GetSize(String self) {
		return self.length();
	}
	public final static String Get(String self, long Index) {
		return String.valueOf(self.charAt((int)Index));
	}
	public final static String Slice(String self, long BIndex) {
		int bindex = (BIndex < 0) ? self.length() - (int)BIndex : (int)BIndex;
		return self.substring(bindex);
	}
	public final static String Slice(String self, long BIndex, long EIndex) {
		int bindex = (BIndex < 0) ? self.length() - (int)BIndex : (int)BIndex;
		int eindex = (EIndex < 0) ? self.length() - (int)EIndex : (int)EIndex;
		return self.substring(bindex, eindex);
	}
	public final static long ToInt(GtType Type, String value) {
		if(value != null) {
			try {
				return Long.parseLong(value);
			}
			catch(NumberFormatException e) {
			}
		}
		return 0;
	}
	public final static double ToDouble(GtType Type, String value) {
		if(value != null) {
			try {
				return Double.parseDouble(value);
			}
			catch(NumberFormatException e) {
			}
		}
		return 0;
	}
	// java original converter between char and String
	public final static String c2s(GtType Type, char ch) {
		return String.valueOf(ch);
	}
	public final static char s2c(GtType Type, String s) {
		return s == null ? (char)0 : s.charAt(0);
	}
}
