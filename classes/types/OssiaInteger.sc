/*
 * This project is a fork of Pierre Cohard's ossia-supercollider
 * https://github.com/OSSIA/ossia-supercollider.git
 * Form his sclang files, the aim is to provide the same message structure
 * specific to the OSSIA library (https://github.com/OSSIA/libossia.git)
 * and the interactive sequencer OSSIA/score (https://github.com/OSSIA/score.git)
 */

+ Integer
{
	*ossiaWsWrite
	{ | anOssiaParameter, ws |

		ws.writeOsc(anOssiaParameter.path, anOssiaParameter.value);
	}

	*ossiaSendMsg
	{ | anOssiaParameter, addr |

		addr.sendRaw(([anOssiaParameter.path] ++ anOssiaParameter.value).asRawOSC);
	}

	*ossiaBounds
	{ | mode |

		switch (mode,
			'free',
			{ ^{ | value, domain | value.asInteger } },
			'clip',
			{ ^{ | value, domain | value.clip(domain.min, domain.max).asInteger } },
			'low',
			{ ^{ | value, domain | value.max(domain.min).asInteger } },
			'high',
			{ ^{ | value, domain | value.min(domain.max).asInteger } },
			'wrap',
			{ ^{ | value, domain | value.wrap(domain.min, domain.max).asInteger } },
			'fold',
			{ ^{ | value, domain | value.fold(domain.min, domain.max).asInteger } },
			'values',
			{
				^{ | value, domain |

					domain[2].detect({ | item | item == value.asInteger });
				};
			},
			{ ^Error("bounding mode not recognized").throw };
		);
	}

	*ossiaNaNFilter
	{ | newVal, oldval |

		if (newVal.isNil) { newVal }
		{ if (newVal.isNaN) { ^oldval } { ^newVal } };
	}

	*ossiaJson { ^"\"i\"" }

	*ossiaDefaultValue { ^0 }
}
