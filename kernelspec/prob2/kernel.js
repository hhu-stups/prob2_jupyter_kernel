define([
	"codemirror/lib/codemirror",
	"codemirror/mode/groovy/groovy",
], function(
	CodeMirror,
) {
	"use strict";
	
	return {
		onload: function() {
			console.info("Loading custom kernel.js");
			CodeMirror.defineMode("classicalb", function(config, parserConfig) {
				return {
					startState: function() {
						return {
							state: "initial",
						};
					},
					
					token: function(stream, state) {
						switch (state.state) {
							case "initial":
								if (stream.match(/^\/\//)) {
									// Line comment found, consume the rest of the line.
									stream.match(/^.+/);
									return "comment";
								} else if (stream.match(/^\/\*/)) {
									// Block comment start found, switch to comment state.
									state.state = "comment";
									return "comment";
								} else if (stream.match(/^"(?:[^\\"\n]|\\[^\n])*"/)) {
									return "string";
								} else if (stream.match(/^"""/)) {
									// Multiline string found, switch to string state.
									state.state = "string";
									return "string";
								} else if (stream.match(/^(?:[0-9]+|0x[0-9A-Fa-f]+)/)) {
									return "number";
								} else if (stream.match(/^(?:[⋂∏∑⋃]|(?:ABSTRACT|CONCRETE)_(?:CONSTANTS|VARIABLES)|ANY|ASSERT|ASSERTIONS|BE|BEGIN|CASE|CHOICE|CONSTANTS|CONSTRAINTS|DEFINITIONS|EXPRESSIONS|PREDICATES|DO|EITHER|ELSE|ELSIF|END|EXTENDS|IF|IMPLEMENTATION|IMPORTS|IN|INCLUDES|INITIALI[SZ]ATION|INTER|INVARIANT|LET|LOCAL_OPERATIONS|MACHINE|MODEL|SYSTEM|OF|OPERATIONS|EVENTS|OR|PI|PRE|PROMOTES|PROPERTIES|REFINES|REFINEMENT|SEES|SELECT|SETS|SET|SIGMA|THEN|UNION|USES|VALUES|VAR|VARIANT|VARIABLES|WHEN|WHERE|WHILE|skip|FREETYPES)/)) {
									return "keyword";
								} else if (stream.match(/^(?:[⊥ℤℕ⊤∅]|BOOL|bfalse|FALSE|INT|INTEGER|MAXINT|MININT|NAT|NAT1|NATURAL|NATURAL1|ℕ1|ℕ₁|STRING|TRUE)/)) {
									return "atom";
								} else if (stream.match(/^(?:[ℙ¬]|FIN|FIN1|POW|POW1|ℙ1|ℙ₁|arity|bin|bool|btree|card|closure|closure1|conc|const|dom|father|first|fnc|front|id|infix|inter|iseq|iseq1|iterate|last|left|max|min|mirror|not|perm|postfix|pred|prefix|prj1|prj2|rank|ran|rec|rel|rev|right|seq|seq1|sizet|size|sons|son|struct|subtree|succ|tail|top|tree|union)/)) {
									return "builtin";
								} else if (stream.match(/^(?:[!∀#∃$%λ&∧'\*×\+⇸⤀\-−→↠⇾\.·‥\/÷∉⊈⊄≠\\∩↑:∈;<\ue103⋖↔⇽←⊆⊂⩤◀≤⇔◁=⇒>⤔↣⤖⊗≥∪↓\^⌒∨\|∣∥↦▷⩥▶~∼\ue100\ue101\ue102]|\*\*|\+[\-−]>|\+[\-−]>>|[\-−][\-−]>|[\-−][\-−]>>|[\-−]>|\.\.|\/:|\/<:|\/<<:|\/=|\/\\|\/\|\\|::|:∈|:=|<\+|<[\-−]>|<[\-−]|<[\-−][\-−]|<:|<<:|<<\||<=|<=>|>\||==|=>|>\+>|>[\-−]>|>\+>>|>[\-−]>>|><|>=|\\\/|\\\|\/)|mod|or|\|\||\|[\-−]>|\|>|\|>>|⁻¹|<<[\-−]>|<[\-−]>>|<<[\-−]>>/)) {
									return "operator";
								} else if (stream.match(/^[A-Za-z_][A-Za-z0-9_]*/)) {
									return "variable";
								} else if (stream.match(/^[\s()\[\]{},]+/)) {
									return null;
								} else {
									stream.match(/^.+/);
									return "error";
								}
							
							case "comment":
								while (!stream.eol()) {
									// Consume everything that is not an asterisk.
									stream.match(/^[^\*\n]+/);
									if (stream.match(/^\*\//)) {
										// Asterisk and slash found, switch back to initial state.
										state.state = "initial";
										return "comment";
									} else {
										// Asterisk without slash found, consume and stay in comment state.
										stream.match(/^\*/);
									}
								}
								return "comment";
							
							case "string":
								if (stream.match(/(?:[^\\"\n]|\\[^\n])*"""/)) {
									// End of multiline string found, switch back to initial state.
									stream.state = "initial";
								} else {
									// No end of multiline string found, consume the rest of the line.
									stream.match(/.+/);
								}
								return "string";
							
							default:
								throw new Error("Unhandled state: " + state.state);
						}
					},
				};
			});
			
			CodeMirror.defineMode("prob2_jupyter_repl", function(config, parserConfig) {
				const switchToMode = function(state, mode) {
					state.state = "inner";
					state.innerMode = CodeMirror.getMode(config, mode);
					state.innerState = CodeMirror.startState(state.innerMode);
				};
				
				return {
					startState: function() {
						return {
							state: "command",
							innerMode: null,
							innerState: null,
						};
					},
					
					copyState: function(state) {
						return {
							state: state.state,
							innerMode: state.innerMode,
							innerState: state.innerState === null ? null : CodeMirror.copyState(state.innerMode, state.innerState),
						};
					},
					
					innerMode: function(state) {
						if (state.innerMode === null) {
							return null;
						} else {
							return {mode: state.innerMode, state: state.innerState};
						}
					},
					
					token: function(stream, state) {
						// Consume leading whitespace.
						stream.match(/^\s+/);
						if (stream.eol()) {
							return null;
						}
						switch (state.state) {
							case "command":
								// Initial state: try to consume a command name.
								const command = stream.match(/^\:[^\s]*/);
								if (command) {
									// Command found, switch to the appropriate state or mode.
									switch (command[0]) {
										case "::load":
										case ":constants":
										case ":eval":
										case ":exec":
										case ":init":
										case ":initialise":
										case ":prettyprint":
										case ":solve":
										case ":table":
										case ":type":
											switchToMode(state, "classicalb");
											break;
										
										case "::render":
											state.state = "render_mimetype";
											break;
										
										case ":groovy":
											switchToMode(state, "groovy");
											break;
										
										case ":time":
											state.state = "command";
											break;
										
										default:
											switchToMode(state, null);
									}
									return "meta";
								} else {
									// No command found, switch to B mode.
									switchToMode(state, "classicalb");
									return state.innerMode.token(stream, state.innerState);
								}
							
							case "render_mimetype":
								// Try to consume the MIME type argument of ::render.
								const mimetype = stream.match(/^\S+/);
								if (mimetype) {
									// MIME type found, try to switch to the appropriate mode.
									switchToMode(state, mimetype[0]);
								} else {
									// No MIME type found, switch to plaintext mode.
									switchToMode(state, null);
								}
								break;
							
							case "inner":
								// If an inner mode is active, delegate to its token method.
								return state.innerMode.token(stream, state.innerState);
							
							default:
								throw new Error("Unhandled state: " + state.state);
						}
					},
				};
			});
			
			CodeMirror.defineMIME("text/x-classicalb", "classicalb");
			CodeMirror.defineMIME("text/x-prob2-jupyter-repl", "prob2_jupter_repl");
			
			// CodeMirror doesn't understand the text/latex MIME type by default.
			CodeMirror.defineMIME("text/latex", "stex");
		},
	};
});
