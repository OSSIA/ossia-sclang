/*
* This project is a fork of Pierre Cohard's ossia-supercollider
* https://github.com/OSSIA/ossia-supercollider.git
* Form his sclang files, the aim is to provide the same message structure
* specific to the OSSIA library (https://github.com/OSSIA/libossia.git)
* and the interactive sequencer OSSIA/score (https://github.com/OSSIA/score.git)
*/

//-------------------------------------------//
//               OSSIA_VECNF                 //
//-------------------------------------------//

OSSIA_FVector {

	var <am_val, m_sz;

	*new {|sz ... values|
		^super.new.init(sz, values);
	}

	init { |sz, v|

		v.do({|item|
			if((item.isFloat.not) && (item.isInteger.not)) {
				Error("OSSIA: Error! Arguments are not of Float type").throw;
			};
		});

		am_val = v;
		m_sz = sz;
	}

	at {|i| ^am_val[i] }
	put { |index, item| am_val[index] = item.asFloat }

	*ossiaSendMsg {	|anOssiaParameter, addr|
		addr.sendRaw(([anOssiaParameter.path] ++ anOssiaParameter.value).asRawOSC);
	}

	*ossiaWsWrite {	|anOssiaParameter, ws|
		var msg = [anOssiaParameter.path] ++ anOssiaParameter.value;
		ws.writeOsc(*msg);
	}

	*ossiaBounds { |mode|
		switch(mode,
			'free', {
				^{ |value, domain| this.asOssiaVec(value) };
			},
			'clip', {
				^{ |value, domain| this.asOssiaVec(
					value.collect({ |item, i|
						item.clip(domain.min[i], domain.max[i]);
					});
				)};
			},
			'low', {
				^{ |value, domain| this.asOssiaVec(
					value.collect({ |item, i|
						item.max(domain.min[i])};
					);
				)};
			},
			'high', {
				^{ |value, domain| this.asOssiaVec(
					value.collect({ |item, i|
						item.min(domain.max[i]);
					});
				)};
			},
			'wrap', {
				^{ |value, domain| this.asOssiaVec(
					value.collect({ |item, i|
						item.wrap(domain.min[i], domain.max[i]);
					});
				)};
			},
			'fold', {
				^{ |value, domain| this.asOssiaVec(
					value.collect({ |item, i|
						item.fold(domain.min[i], domain.max[i]);
					});
				)};
			}, {
				^{ |value, domain| domain[2].detect({ |item|
					item == this.asOssiaVec(value) });
				};
		});
	}
}

OSSIA_vec2f : OSSIA_FVector {

	*new {|v1 = 0.0, v2 = 0.0|
		^super.new(2, v1.asFloat, v2.asFloat);
	}

	*asOssiaVec { |anArray|
		^[anArray[0].asFloat, anArray[1].asFloat];
	}

	*ossiaDefaultValue { ^[0.0, 0.0]; }

	*ossiaNaNFilter { |newVal, oldval|
		^[if (newVal[0].isNil) { newVal[0] }
			{ if (newVal[0].isNaN) { oldval[0] } { newVal[0] }},
			if (newVal[1].isNil) { newVal[1] }
			{ if (newVal[1].isNaN) { oldval[1] } { newVal[1] }} ];
	}

	*ossiaJson { ^"\"ff\"" }

	*ossiaWidget { |anOssiaParameter|
		var isCartesian = false, event;

		if (anOssiaParameter.unit.notNil) {
			if (anOssiaParameter.unit.string == "position.cart2D") { isCartesian = true; };
		};

		if (isCartesian) {
			var specX = ControlSpec(), specY = ControlSpec();

			event = { | param |
				{
					if (param.value != [param.widgets[0].value,
						param.widgets[1].value]) {
						param.widgets[0].value_(param.value[0]);
						param.widgets[1].value_(param.value[1]);
					};

					if (param.value != [specX.map(param.widgets[2].x),
						specY.map(param.widgets[2].y)]
					) {
						param.widgets[2].x_(specX.unmap(param.value[0]));
						param.widgets[2].y_(specY.unmap(param.value[1]));
					};
				}.defer;
			};

			anOssiaParameter.widgets = [
				EZNumber(anOssiaParameter.window, 244@20, anOssiaParameter.name,
					action:{ | val | anOssiaParameter.value_([
						val.value,
						anOssiaParameter.value[1]
				])},labelWidth:100,
					initVal:anOssiaParameter.value[0],
					gap:4@0),
				EZNumber(anOssiaParameter.window, 144@20,
					action:{ | val | anOssiaParameter.value_([
						anOssiaParameter.value[0],
						val.value
				])}, initVal:anOssiaParameter.value[1],
					gap:0@0)
			];

			anOssiaParameter.widgets.do({ | item |
				item.setColors(stringColor:OSSIA.pallette.color('baseText', 'active'),
					numNormalColor:OSSIA.pallette.color('windowText', 'active'));

				// set numberBoxes scroll step and colors
				item.numberView.maxDecimals_(3)
				.step_(0.001).scroll_step_(0.001);
			});

			if (anOssiaParameter.domain.min.notNil) {
				anOssiaParameter.widgets[0].controlSpec.minval_(anOssiaParameter.domain.min[0]);
				anOssiaParameter.widgets[1].controlSpec.minval_(anOssiaParameter.domain.min[1]);
				anOssiaParameter.widgets[0].controlSpec.maxval_(anOssiaParameter.domain.max[0]);
				anOssiaParameter.widgets[1].controlSpec.maxval_(anOssiaParameter.domain.max[1]);
				specX.minval_(anOssiaParameter.domain.min[0]);
				specY.minval_(anOssiaParameter.domain.min[1]);
				specX.maxval_(anOssiaParameter.domain.max[0]);
				specY.maxval_(anOssiaParameter.domain.max[1]);
			};

			anOssiaParameter.widgets = anOssiaParameter.widgets ++ Slider2D(anOssiaParameter.window, 392@392)
			.x_(specX.unmap(anOssiaParameter.value[0])) // initial location of x
			.y_(specY.unmap(anOssiaParameter.value[1])) // initial location of y
			.action_({ | val | anOssiaParameter.value_([
				specX.map(val.x),
				specY.map(val.y)
			])
			}).onClose_({ anOssiaParameter.removeDependant(event); });

			anOssiaParameter.widgets[2].focusColor_(
				OSSIA.pallette.color('midlight', 'active'))
			.background_(
				OSSIA.pallette.color('middark', 'active'))
			.knobColor_(OSSIA.pallette.color('light', 'active'));

		} {

			event = { | param |
				{
					if (param.value != param.widgets.value) {
						param.widgets.value_(param.value);
					};
				}.defer;
			};

			anOssiaParameter.widgets = EZRanger(anOssiaParameter.window, 392@20, anOssiaParameter.name,
				action:{ | val | anOssiaParameter.value_(val.value); },
				initVal: anOssiaParameter.value,
				labelWidth:100,
				initVal: anOssiaParameter.value,
				gap:4@0).onClose_({ anOssiaParameter.removeDependant(event);
			}).setColors(
				stringColor:OSSIA.pallette.color('baseText', 'active'),
				sliderColor:OSSIA.pallette.color('middark', 'active'),
				numNormalColor:OSSIA.pallette.color('windowText', 'active'),
				knobColor:OSSIA.pallette.color('light', 'active')
			);

			anOssiaParameter.widgets.rangeSlider.focusColor_(
				OSSIA.pallette.color('midlight', 'active'));

			anOssiaParameter.widgets.hiBox.maxDecimals_(3)
			.step_(0.001).scroll_step_(0.001);

			anOssiaParameter.widgets.loBox.maxDecimals_(3)
			.step_(0.001).scroll_step_(0.001);

			if (anOssiaParameter.domain.min.notNil) {
				anOssiaParameter.widgets.controlSpec.minval_(anOssiaParameter.domain.min[0]);
				anOssiaParameter.widgets.controlSpec.maxval_(anOssiaParameter.domain.max[1]);
			};
		};

		anOssiaParameter.addDependant(event);
	}
}

OSSIA_vec3f : OSSIA_FVector {

	*new {|v1 = 0.0, v2 = 0.0, v3 = 0.0|
		^super.new(3, v1.asFloat, v2.asFloat, v3.asFloat);
	}

	*asOssiaVec { |anArray|
		^[anArray[0].asFloat, anArray[1].asFloat, anArray[2].asFloat];
	}

	*ossiaDefaultValue { ^[0.0, 0.0, 0.0]; }

	*ossiaNaNFilter { |newVal, oldval|
		^[if (newVal[0].isNil) { newVal[0] }
			{ if (newVal[0].isNaN) { oldval[0] } { newVal[0] }},
			if (newVal[1].isNil) { newVal[1] }
			{ if (newVal[1].isNaN) { oldval[1] } { newVal[1] }},
			if (newVal[2].isNil) { newVal[2] }
			{ if (newVal[2].isNaN) { oldval[2] } { newVal[2] }},];
	}

	*ossiaJson { ^"\"fff\"" }

	*ossiaWidget { |anOssiaParameter|
		var isCartesian = false, event;

		anOssiaParameter.widgets = [
			EZNumber(anOssiaParameter.window, 196@20, anOssiaParameter.name,
				action:{ | val | anOssiaParameter.value_([
					val.value,
					anOssiaParameter.value[1],
					anOssiaParameter.value[2]
			])}, labelWidth:100,
				initVal:anOssiaParameter.value[0],
				gap:4@0),
			EZNumber(anOssiaParameter.window, 94@20,
				action:{ | val | anOssiaParameter.value_([
					anOssiaParameter.value[0],
					val.value,
					anOssiaParameter.value[2],
			])}, initVal:anOssiaParameter.value[1],
				gap:0@0),
			EZNumber(anOssiaParameter.window, 94@20,
				action:{ | val | anOssiaParameter.value_([
					anOssiaParameter.value[0],
					anOssiaParameter.value[1],
					val.value
			])}, initVal:anOssiaParameter.value[2],
				gap:0@0).onClose_({ anOssiaParameter.removeDependant(event); });
		];

		anOssiaParameter.widgets.do({ | item |
			item.setColors(stringColor:OSSIA.pallette.color('baseText', 'active'),
			numNormalColor:OSSIA.pallette.color('windowText', 'active'));

			// set numberBoxes scroll step and colors
			item.numberView.maxDecimals_(3)
			.step_(0.001).scroll_step_(0.001);
		});

		if (anOssiaParameter.unit.notNil) {
			if (anOssiaParameter.unit.string == "position.cart3D") { isCartesian = true; };
		};

		if (isCartesian) {
			var specX = ControlSpec(), specY = ControlSpec(), specZ = ControlSpec(), sliders;

			if(anOssiaParameter.domain.min.notNil) {
				anOssiaParameter.widgets[0].controlSpec.minval_(anOssiaParameter.domain.min[0]);
				anOssiaParameter.widgets[1].controlSpec.minval_(anOssiaParameter.domain.min[1]);
				anOssiaParameter.widgets[0].controlSpec.maxval_(anOssiaParameter.domain.max[0]);
				anOssiaParameter.widgets[1].controlSpec.maxval_(anOssiaParameter.domain.max[1]);
				anOssiaParameter.widgets[2].controlSpec.minval_(anOssiaParameter.domain.min[2]);
				anOssiaParameter.widgets[2].controlSpec.maxval_(anOssiaParameter.domain.max[2]);
				specX.minval_(anOssiaParameter.domain.min[0]);
				specY.minval_(anOssiaParameter.domain.min[1]);
				specX.maxval_(anOssiaParameter.domain.max[0]);
				specY.maxval_(anOssiaParameter.domain.max[1]);
				specZ.minval_(anOssiaParameter.domain.min[2]);
				specZ.maxval_(anOssiaParameter.domain.max[2]);
			};

			sliders = [
				Slider2D(anOssiaParameter.window, 368@368)
				.x_(specX.unmap(anOssiaParameter.value[0])) // initial value of x
				.y_(specY.unmap(anOssiaParameter.value[1])) // initial value of y
				.action_({ | val | anOssiaParameter.value_(
					[
						specX.map(val.x),
						specY.map(val.y),
						anOssiaParameter.value[2]
					]
				)}),
				Slider(anOssiaParameter.window, 20@368)
				.orientation_(\vertical)
				.value_(specZ.unmap(anOssiaParameter.value[2])) // initial value of z
				.action_({ | val | anOssiaParameter.value_(
					[
						anOssiaParameter.value[0],
						anOssiaParameter.value[1],
						specZ.map(val.value)
					]
				)})
			];

			sliders.do({ | item |
				item.focusColor_(
					OSSIA.pallette.color('midlight', 'active'))
				.background_(
					OSSIA.pallette.color('middark', 'active'))
				.knobColor_(OSSIA.pallette.color('light', 'active'));
			});

			anOssiaParameter.widgets = anOssiaParameter.widgets ++ sliders;

			event = { | param |
				{
					if (param.value != [param.widgets[0].value,
						param.widgets[1].value,
						param.widgets[2].value]) {
						param.widgets[0].value_(param.value[0]);
						param.widgets[1].value_(param.value[1]);
						param.widgets[2].value_(param.value[2]);
					};

					if (param.value != [specX.map(param.widgets[3].x),
						specY.map(param.widgets[3].y),
						specZ.map(param.widgets[4].value)]
					) {
						param.widgets[3].x_(specX.unmap(param.value[0]));
						param.widgets[3].y_(specY.unmap(param.value[1]));
						param.widgets[4].value_(specZ.unmap(param.value[2]));
					};
				}.defer;
			};

		} {

			if(anOssiaParameter.domain.min.notNil) {
				anOssiaParameter.widgets[0].controlSpec.minval_(anOssiaParameter.domain.min[0]);
				anOssiaParameter.widgets[0].controlSpec.maxval_(anOssiaParameter.domain.max[0]);
				anOssiaParameter.widgets[1].controlSpec.minval_(anOssiaParameter.domain.min[1]);
				anOssiaParameter.widgets[1].controlSpec.maxval_(anOssiaParameter.domain.max[1]);
				anOssiaParameter.widgets[2].controlSpec.minval_(anOssiaParameter.domain.min[2]);
				anOssiaParameter.widgets[2].controlSpec.maxval_(anOssiaParameter.domain.max[2]);
			};

			event = { | param |
				{
					if (param.value != [param.widgets[0].value,
						param.widgets[1].value,
						param.widgets[2].value]) {
						param.widgets[0].value_(param.value[0]);
							param.widgets[1].value_(param.value[1]);
							param.widgets[2].value_(param.value[2]);
					}
				}.defer;
			};
		};

		anOssiaParameter.addDependant(event);
	}
}

OSSIA_vec4f : OSSIA_FVector {

	*new {|v1 = 0.0, v2 = 0.0, v3 = 0.0, v4 = 0.0|
		^super.new(4, v1.asFloat, v2.asFloat, v3.asFloat, v4.asFloat);
	}

	*asOssiaVec { |anArray|
		^[anArray[0].asFloat, anArray[1].asFloat,  anArray[2].asFloat,  anArray[3].asFloat];
	}

	*ossiaDefaultValue { ^[0.0, 0.0, 0.0, 0.0]; }

	*ossiaNaNFilter { |newVal, oldval|
		^[if (newVal[0].isNil) { newVal[0] }
			{ if (newVal[0].isNaN) { oldval[0] } { newVal[0] }},
			if (newVal[1].isNil) { newVal[1] }
			{ if (newVal[1].isNaN) { oldval[1] } { newVal[1] }},
			if (newVal[2].isNil) { newVal[2] }
			{ if (newVal[2].isNaN) { oldval[2] } { newVal[2] }},
			if (newVal[3].isNil) { newVal[3] }
			{ if (newVal[3].isNaN) { oldval[3] } { newVal[3] }} ];
	}

	*ossiaJson { ^"\"ffff\"" }

	*ossiaWidget { |anOssiaParameter|

		var event = { | param |
			{
				if (param.value != [param.widgets[0].value,
					param.widgets[1].value,
					param.widgets[2].value,
					param.widgets[3].value]) {
					param.widgets[0].value_(param.value[0]);
					param.widgets[1].value_(param.value[1]);
					param.widgets[2].value_(param.value[2]);
					param.widgets[2].value_(param.value[3]);
				}
			}.defer;
		};

		anOssiaParameter.widgets = [
			EZNumber(anOssiaParameter.window, 170@20, anOssiaParameter.name,
				action:{ | val | anOssiaParameter.value_([
					val.value,
					anOssiaParameter.value[1],
					anOssiaParameter.value[2],
					anOssiaParameter.value[3]
				])}, labelWidth:100,
				initVal: anOssiaParameter.value[0],
				gap:4@0),
			EZNumber(anOssiaParameter.window, 70@20,
				action:{ | val | anOssiaParameter.value_([
					anOssiaParameter.value[0],
					val.value,
					anOssiaParameter.value[2],
					anOssiaParameter.value[3]
				])}, initVal: anOssiaParameter.value[1],
				gap:0@0),
			EZNumber(anOssiaParameter.window, 70@20,
				action:{ | val | anOssiaParameter.value_([
					anOssiaParameter.value[0],
					anOssiaParameter.value[1],
					val.value,
					anOssiaParameter.value[3]
				])}, initVal: anOssiaParameter.value[2],
				gap:0@0),
			EZNumber(anOssiaParameter.window, 70@20,
				action:{ | val | anOssiaParameter.value_([
					anOssiaParameter.value[0],
					anOssiaParameter.value[1],
					anOssiaParameter.value[2],
					val.value
				])}, initVal: anOssiaParameter.value[3],
				gap:0@0).onClose_({ anOssiaParameter.removeDependant(event); });
		];

		anOssiaParameter.widgets.do({ | item |
			item.setColors(stringColor:OSSIA.pallette.color('baseText', 'active'),
				numNormalColor:OSSIA.pallette.color('windowText', 'active'));

			// set numberBoxes scroll step and colors
			item.numberView.maxDecimals_(3)
			.step_(0.001).scroll_step_(0.001);
		});

		if(anOssiaParameter.domain.min.notNil) {
			anOssiaParameter.widgets[0].controlSpec.minval_(anOssiaParameter.domain.min[0]);
			anOssiaParameter.widgets[0].controlSpec.maxval_(anOssiaParameter.domain.max[0]);
			anOssiaParameter.widgets[1].controlSpec.minval_(anOssiaParameter.domain.min[1]);
			anOssiaParameter.widgets[1].controlSpec.maxval_(anOssiaParameter.domain.max[1]);
			anOssiaParameter.widgets[2].controlSpec.minval_(anOssiaParameter.domain.min[2]);
			anOssiaParameter.widgets[2].controlSpec.maxval_(anOssiaParameter.domain.max[2]);
			anOssiaParameter.widgets[3].controlSpec.minval_(anOssiaParameter.domain.min[3]);
			anOssiaParameter.widgets[3].controlSpec.maxval_(anOssiaParameter.domain.max[3]);
		};
	}
}