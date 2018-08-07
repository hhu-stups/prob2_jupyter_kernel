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
				const keywordClasses = {
					ABSTRACT_CONSTANTS: "keyword",
					ABSTRACT_VARIABLES: "keyword",
					ANY: "keyword",
					ASSERT: "keyword",
					ASSERTIONS: "keyword",
					BE: "keyword",
					BEGIN: "keyword",
					BOOL: "atom",
					bfalse: "atom",
					CASE: "keyword",
					CHOICE: "keyword",
					CONCRETE_CONSTANTS: "keyword",
					CONCRETE_VARIABLES: "keyword",
					CONSTANTS: "keyword",
					CONSTRAINTS: "keyword",
					DEFINITIONS: "keyword",
					EXPRESSIONS: "keyword",
					PREDICATES: "keyword",
					DO: "keyword",
					EITHER: "keyword",
					ELSE: "keyword",
					ELSIF: "keyword",
					END: "keyword",
					EXTENDS: "keyword",
					FALSE: "atom",
					FIN: "builtin",
					FIN1: "builtin",
					IF: "keyword",
					IMPLEMENTATION: "keyword",
					IMPORTS: "keyword",
					IN: "keyword",
					INCLUDES: "keyword",
					INITIALISATION: "keyword",
					INITIALIZATION: "keyword",
					INT: "atom",
					INTEGER: "atom",
					INTER: "keyword",
					INVARIANT: "keyword",
					LET: "keyword",
					LOCAL_OPERATIONS: "keyword",
					MACHINE: "keyword",
					MODEL: "keyword",
					SYSTEM: "keyword",
					MAXINT: "atom",
					MININT: "atom",
					NAT: "atom",
					NAT1: "atom",
					NATURAL: "atom",
					NATURAL1: "atom",
					OF: "keyword",
					OPERATIONS: "keyword",
					EVENTS: "keyword",
					OR: "keyword",
					PI: "keyword",
					POW: "builtin",
					POW1: "builtin",
					PRE: "keyword",
					PROMOTES: "keyword",
					PROPERTIES: "keyword",
					REFINES: "keyword",
					REFINEMENT: "keyword",
					SEES: "keyword",
					SELECT: "keyword",
					SETS: "keyword",
					SET: "keyword",
					SIGMA: "keyword",
					STRING: "atom",
					THEN: "keyword",
					TRUE: "atom",
					UNION: "keyword",
					USES: "keyword",
					VALUES: "keyword",
					VAR: "keyword",
					VARIANT: "keyword",
					VARIABLES: "keyword",
					WHEN: "keyword",
					WHERE: "keyword",
					WHILE: "keyword",
					arity: "builtin",
					bin: "builtin",
					bool: "builtin",
					btree: "builtin",
					card: "builtin",
					closure: "builtin",
					closure1: "builtin",
					conc: "builtin",
					const: "builtin",
					dom: "builtin",
					father: "builtin",
					first: "builtin",
					fnc: "builtin",
					front: "builtin",
					id: "builtin",
					infix: "builtin",
					inter: "builtin",
					iseq: "builtin",
					iseq1: "builtin",
					iterate: "builtin",
					last: "builtin",
					left: "builtin",
					max: "builtin",
					min: "builtin",
					mirror: "builtin",
					mod: "operator",
					not: "operator",
					or: "builtin",
					perm: "builtin",
					postfix: "builtin",
					pred: "builtin",
					prefix: "builtin",
					prj1: "builtin",
					prj2: "builtin",
					rank: "builtin",
					ran: "builtin",
					rec: "builtin",
					rel: "builtin",
					rev: "builtin",
					right: "builtin",
					seq: "builtin",
					seq1: "builtin",
					sizet: "builtin",
					size: "builtin",
					skip: "keyword",
					sons: "builtin",
					son: "builtin",
					struct: "builtin",
					subtree: "builtin",
					succ: "builtin",
					tail: "builtin",
					top: "builtin",
					tree: "builtin",
					union: "builtin",
					FREETYPES: "keyword",
				};
				
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
								} else if (stream.match(/^[⋂∏∑⋃]/)) {
									return "keyword";
								} else if (stream.match(/^(?:[⊥ℤℕ⊤∅]|ℕ1|ℕ₁)/)) {
									return "atom";
								} else if (stream.match(/^(?:[ℙ¬]|ℙ1|ℙ₁)/)) {
									return "builtin";
								} else if (stream.match(/^(?:[!∀#∃$%λ&∧'\*×\+⇸⤀\-−→↠⇾\.·‥\/÷∉⊈⊄≠\\∩↑:∈;<\ue103⋖↔⇽←⊆⊂⩤◀≤⇔◁=⇒>⤔↣⤖⊗≥∪↓\^⌒∨\|∣∥↦▷⩥▶~∼\ue100\ue101\ue102]|\*\*|\+[\-−]>|\+[\-−]>>|[\-−][\-−]>|[\-−][\-−]>>|[\-−]>|\.\.|\/:|\/<:|\/<<:|\/=|\/\\|\/\|\\|::|:∈|:=|<\+|<[\-−]>|<[\-−]|<[\-−][\-−]|<:|<<:|<<\||<=|<=>|>\||==|=>|>\+>|>[\-−]>|>\+>>|>[\-−]>>|><|>=|\\\/|\\\|\/)|\|\||\|[\-−]>|\|>|\|>>|⁻¹|<<[\-−]>|<[\-−]>>|<<[\-−]>>/)) {
									return "operator";
								} else if (stream.match(/^[\s()\[\]{},]+/)) {
									return null;
								} else {
									const variable = stream.match(/^[A-Za-z_][A-Za-z0-9_]*/);
									if (variable) {
										// Check if the variable name is actually a keyword that should be highlighted differently.
										if (variable in keywordClasses) {
											return keywordClasses[variable];
										} else {
											return "variable";
										}
									} else {
										stream.match(/^.+/);
										return "error";
									}
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
