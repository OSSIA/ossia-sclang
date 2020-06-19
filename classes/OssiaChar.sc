/*
 * This project is a fork of Pierre Cohard's ossia-supercollider
 * https://github.com/OSSIA/ossia-supercollider.git
 * Form his sclang files, the aim is to provide the same message structure
 * specific to the OSSIA library (https://github.com/OSSIA/libossia.git)
 * and the interactive sequencer OSSIA/score (https://github.com/OSSIA/score.git)
 */

+ Char {

	*ossiaWsWrite {	|anOssiaParameter, ws|
		ws.writeOsc(anOssiaParameter.path, anOssiaParameter.value);
	}

	*ossiaSendMsg {	|anOssiaParameter, addr|
		addr.sendRaw(([anOssiaParameter.path] ++ anOssiaParameter.value.asSymbol).asRawOSC);
	}

	*ossiaBounds { |mode|
		switch(mode,
			'free', {
				^{ |value, domain| value.asAscii };
			},
			'clip', {
				{ |value, domain| value.clip(domain.min, domain.max).asAscii };
			},
			'low', {
				^{ |value, domain| value.max(domain.min).asAscii };
			},
			'high', {
				^{ |value, domain| value.max(domain.min).asAscii };
			},
			'wrap', {
				^{ |value, domain, type| value.wrap(domain.min, domain.max).asAscii };
			},
			'fold', {
				^{ |value, domain, type| value.fold(domain.min, domain.max).asAscii }
			}, {
				^{ |value, domain, type| domain[2].detect({ |item|
					item == value.asAscii });
				};
		});
	}

	*ossiaDefaultValue { ^$ ; }

	*ossiaNaNFilter { |newVal, oldval|
		^newVal;
	}

	*ossiaJson { ^"\"c\"" }

	*ossiaWidget { |anOssiaParameter|
		var widgets, enventName = (anOssiaParameter.device.name ++ anOssiaParameter.path).asSymbol;
		// apend the device Name to diferentiate between multiple devices with identical parameters
		// ei. server and mirror

		widgets = EZText(anOssiaParameter.window, 392@20, anOssiaParameter.name,
			action:{ | val | anOssiaParameter.value_(val.value); },
			initVal: anOssiaParameter.value, labelWidth:100, gap:4@0).onClose_({
			anOssiaParameter.removeFromEvenGui_(enventName); });

		anOssiaParameter.addToEvenGui_(
			enventName,
			{
				if (anOssiaParameter.value != widgets.value) {
					widgets.value_(anOssiaParameter.value);
				};
			};
		);
	}
}