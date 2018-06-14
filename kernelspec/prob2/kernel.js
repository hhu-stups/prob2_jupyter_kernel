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
						if (stream.match(/^[A-Z_]+/)) {
							return "keyword";
						} else {
							stream.match(/^.+/);
							return null;
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
