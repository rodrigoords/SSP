/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
Ext.define('Ssp.controller.tool.map.SemesterPanelContainerViewController', {
    extend: 'Deft.mvc.ViewController',
    mixins: [ 'Deft.mixin.Injectable' ],
    inject:{
		appEventsController: 'appEventsController',
    	termsStore:'termsStore',
    	mapPlanService:'mapPlanService',
		formUtils: 'formRendererUtils',
		person: 'currentPerson',
		authenticatedPerson: 'authenticatedPerson',
        personLite: 'personLite',
    	currentMapPlan: 'currentMapPlan'
    },
    
	control: {
	    	view: {
				afterlayout: {
					fn: 'onAfterLayout',
					single: true
				},
	    	},
	},

	semesterStores: [],
	init: function() {
		var me=this;
		var id = me.personLite.get('id');
	    me.resetForm();
		if(me.termsStore.getTotalCount() == 0)
			me.termsStore.load();
		me.appEventsController.assignEvent({eventName: 'onCreateNewMapPlan', callBackFunc: me.onCreateNewMapPlan, scope: me});
		me.appEventsController.assignEvent({eventName: 'onLoadMapPlan', callBackFunc: me.onLoadPlanCompleteMapPlan, scope: me});
		
		me.appEventsController.assignEvent({eventName: 'onSaveMapPlan', callBackFunc: me.onSaveMapPlan, scope: me});
		me.appEventsController.assignEvent({eventName: 'onPrintMapPlan', callBackFunc: me.onPrintMapPlan, scope: me});

		me.appEventsController.assignEvent({eventName: 'updateAllPlanHours', callBackFunc: me.updateAllPlanHours, scope: me});
		me.appEventsController.assignEvent({eventName: 'onViewCourseNotes', callBackFunc: me.onViewCourseNotes, scope: me});
		
		me.termsStore.addListener("load", me.onCreateNewMapPlan, me);
		return me.callParent(arguments);
    },
    resetForm: function() {
        var me = this;
        me.getView().getForm().reset();
    },
    
    newServiceSuccessHandler: function(name, callback, serviceResponses) {
        var me = this;
        return me.newServiceHandler(name, callback, serviceResponses, function(name, serviceResponses, response) {
            serviceResponses.successes[name] = response;
        });
    },

    newServiceFailureHandler: function(name, callback, serviceResponses) {
        var me = this;
        return me.newServiceHandler(name, callback, serviceResponses, function(name, serviceResponses, response) {
            serviceResponses.failures[name] = response;
        });
    },

    newServiceHandler: function(name, callback, serviceResponses, serviceResponsesCallback) {
        return function(r, scope) {
            var me = scope;
            serviceResponses.responseCnt++;
            if ( serviceResponsesCallback ) {
                serviceResponsesCallback.apply(me, [name, serviceResponses, r]);
            }
            if ( callback ) {
                callback.apply(me, [ serviceResponses ]);
            }
            me.afterServiceHandler(serviceResponses);
        };
    },

    getMapPlanServiceSuccess: function(serviceResponses) {
        var me = this;
        var mapResponse = serviceResponses.successes.map;
		if(!mapResponse || !mapResponse.responseText || mapResponse.responseText.trim().length == 0) {
			if(me.termsStore.isLoading()) {
				 me.termsStore.on('load', this.getMapPlanServiceFailure, this, {single: true});
				return;
			}
			me.getMapPlanServiceFailure();
       	} else {
			me.currentMapPlan.populateFromGenericObject(Ext.decode(mapResponse.responseText));
			me.onCreateMapPlan();
			me.populatePlanStores();
			me.updateAllPlanHours();
		}
    },
    onLoadPlanCompleteMapPlan: function () {
    	var me = this;
		me.onCreateMapPlan();
		me.populatePlanStores();
		me.updateAllPlanHours();    	
    },
    getMapPlanServiceFailure: function() {
		var me = this;
		me.onCreateNewMapPlan();
		me.updateAllPlanHours();
		me.currentMapPlan.set('personId',me.personLite.get('id'));
		me.currentMapPlan.set('ownerId',me.authenticatedPerson.get('id'));
    },
 
	onAfterLayout: function(){
		var me = this;
		console.log('onAfterLayout');
		me.getView().setLoading(true);
		var id = me.personLite.get('id');
	    
	    if (id != "") {
			me.getView().setLoading(true);
			var serviceResponses = {
                failures: {},
                successes: {},
                responseCnt: 0,
                expectedResponseCnt: 1
            }
	    	 me.mapPlanService.getCurrent(id, {
	             success: me.newServiceSuccessHandler('map', me.getMapPlanServiceSuccess, serviceResponses),
	             failure: me.newServiceFailureHandler('map', me.getMapPlanServiceFailure, serviceResponses),
	             scope: me
	         });
	    }
		 
	},
	
	getTerms: function(mapPlan){
		var me = this;
		var terms;
		me.semesterStores = [];
		var planCourses = me.currentMapPlan.get('planCourses');
		if(!planCourses || planCourses.length == 0){
			terms = me.termsStore.getCurrentAndFutureTerms(5);
		} else {
			terms = me.termsStore.getTermsFromTermCodes(me.mapPlanService.getTermCodes(mapPlan));
		}
		return terms;
	},
	
	fillSemesterStores: function(terms){
		var me = this;
		terms.forEach(function(term){
			me.semesterStores[term.get('code')] = new Ssp.store.SemesterCourses();
		});
	},
	
	onCreateNewMapPlan:function(){
		var me = this;
		me.currentMapPlan.clearMapPlan();
		me.currentMapPlan.set('personId',  me.personLite.get('id'));
		me.currentMapPlan.set('ownerId',  me.authenticatedPerson.get('id'));
		me.currentMapPlan.set('name','New Plan');
		me.onCreateMapPlan();
	},
	populatePlanStores:function(){
		var me = this;
		var planCourses = me.currentMapPlan.get('planCourses');
		planCourses.forEach(function(planCourse){
			var termStore = me.semesterStores[planCourse.termCode];
			termStore.suspendEvents();
			var semesterCourse = new Ssp.model.tool.map.SemesterCourse(planCourse);
			termStore.add(semesterCourse);
			termStore.resumeEvents();
		}) 
	},
	
	onCreateMapPlan:function(){
		var me = this;
		var view  = me.getView().getComponent("semestersets");
		if(view == null){
			return;
		}
		view.removeAll(true);
		
		var terms = me.getTerms(me.currentMapPlan);
		
		if(terms.length == 0){
			return;
		}
		
		me.fillSemesterStores(terms);

		var termsets = [];
		terms.reverse().forEach(function(term){
			if(termsets.hasOwnProperty(term.get("reportYear"))){
				termsets[term.get("reportYear")][termsets[term.get("reportYear")].length] = term;
			}else{
				termsets[term.get("reportYear")]=[];
				termsets[term.get("reportYear")][0] = term;
			};
		});

		termsets.forEach(function(termSet){
			var yearView = view.add(new Ext.form.FieldSet({
				xtype : 'fieldset',
				border: 0,
				title : '',
				padding : '2 2 2 2',
				margin : '0 0 0 0',
				layout : 'hbox',
				autoScroll : true,
				minHeight: 262,
				itemId : 'year' + termSet[0].get("reportYear"),
				flex : 1,
			}));
		
			termSet.forEach(function(term){
				var termCode = term.get('code');
				var panelName = term.get("name");
				yearView.add(me.createSemesterPanel(panelName, termCode, me.semesterStores[termCode]));
			});
		});
		me.appEventsController.getApplication().fireEvent("onUpdateCurrentMapPlanPlanToolView");
		me.getView().setLoading(false);
    },
	
	createSemesterPanel: function(semesterName, termCode, semesterStore){
		var me = this;
		return new Ssp.view.tools.map.SemesterPanel({
			title:semesterName,
			itemId:termCode,
			store: semesterStore
		});
	},
	
	afterServiceHandler: function(serviceResponses) {
        var me = this;
        if ( serviceResponses.responseCnt >= serviceResponses.expectedResponseCnt ) {
            //me.getView().setLoading(false);
        }
    },

	onSaveMapPlan: function(){
		var me = this;
		me.getView().setLoading(true);
		var callbacks = new Object();
		callbacks.success = me.onSaveCompleteSuccess;
		callbacks.failure = me.onSaveCompleteFailure;
		me.mapPlanService.save(me.semesterStores, callbacks, me.currentMapPlan,me.getView());

	},
	
	onSaveCompleteSuccess: function(view){
		var me = this;
		Ext.Msg.alert('Your changes have been saved.'); 	
		view.setLoading(false);
	},
	
	onSaveCompleteFailure: function(view){
		var me = this;
		view.setLoading(false);
	},	
	
	onPrintMapPlan: function(){
		var me = this;
		me.getView().setLoading(true);
		var serviceResponses = {
                failures: {},
                successes: {},
                responseCnt: 0,
                expectedResponseCnt: 1
            }
		me.mapPlanService.print(me.semesterStores, {
            success: me.newServiceSuccessHandler('printMap', me.printMapPlanServiceSuccess, serviceResponses),
            failure: me.newServiceFailureHandler('printMap', me.printMapPlanServiceFailure, serviceResponses),
            scope: me
        });
	},
	
	 printMapPlanServiceSuccess: function(serviceResponses) {
	        var me = this;
	        var mapResponse = serviceResponses.successes.printMap;
	       	me.onPrintComplete(mapResponse.responseText);
			me.getView().setLoading(false);
	 },

	printMapPlanServiceFailure: function() {
		var me = this;
		me.getView().setLoading(false);
	},
	
	onPrintComplete: function(htmlPrint){
    	var targetElement = Ext.getCmp('PrintablePanelId');
        var myWindow = window.open('', '', 'width=500,height=600');
        myWindow.document.write(htmlPrint);
        myWindow.print();
	},

	updateAllPlanHours: function(){
		var me = this;
		var parent =  me.getView();
		var panels = parent.query("semesterpanel");
		var planHours = 0;
		var devHours = 0;
		panels.forEach(function(panel){
			var store = panel.getStore();
			var semesterBottomDock = panel.getDockedComponent("semesterBottomDock");
			var hours = me.updateTermHours(store, semesterBottomDock);
			planHours += hours.planHours;
			devHours += hours.devHours;
		})
		Ext.getCmp('currentTotalPlanCrHrs').setValue(planHours);
		Ext.getCmp('currentPlanTotalDevCrHrs').setValue(devHours);
		
	},
	
	updateTermHours: function(store, semesterBottomDock){
		var models = store.getRange(0);
		var totalHours = 0;
		var totalDevHours = 0;
		models.forEach(function(model){
			var value = model.get('creditHours');
			/**** TODO the following is in the constructor but drag and drop does not call the constructor *****/
			if(!value){
				value = model.get('minCreditHours');
				model.set('creditHours', value);
			}
			totalHours += value;
			if(model.get('isDev')){
				totalDevHours += value;
			}
		});
		var termCreditHours = semesterBottomDock.getComponent('termCrHrs');
		termCreditHours.setText("" + totalHours + "");
		var hours = new Object();
		hours.planHours = totalHours;
		hours.devHours = totalDevHours;
		return hours;
	},
	
	//TODO This method should probably be at the Semester Panel Level
	onViewCourseNotes: function(args){
		var me = this;
		var courseRecord = args.store.getAt(args.rowIndex);

    	if(me.coursePlanDetails == null || me.coursePlanDetails.isDestroyed){
    		me.coursePlanDetails = Ext.create('Ssp.view.tools.map.CourseNotes');
			var creditHours = me.coursePlanDetails.query('#creditHours')[0];
    		creditHours.setValue(courseRecord.get('creditHours'));
		    creditHours.setMinValue(courseRecord.get('minCreditHours'));
			creditHours.setMaxValue(courseRecord.get('maxCreditHours'));
    		me.coursePlanDetails.rowIndex = args.rowIndex;
    		me.coursePlanDetails.semesterStore = args.store;
			me.coursePlanDetails.setTitle(courseRecord.get('formattedCourse') + ' - ' + courseRecord.get('title'));
    		me.coursePlanDetails.center();
    		me.coursePlanDetails.show();
    	}
	},

	destroy: function() {
        var me=this;
		if(me.coursePlanDetails != null && !me.coursePlanDetails.isDestroyed){
			me.coursePlanDetails.close();
		}
		me.termsStore.removeListener("load", me.onCreateNewMapPlan, me);
		me.appEventsController.removeEvent({eventName: 'onCreateNewMapPlan', callBackFunc: me.onCreateNewMapPlan, scope: me});
        me.appEventsController.removeEvent({eventName: 'onSaveMapPlan', callBackFunc: me.onSaveMapPlan, scope: me});
		me.appEventsController.removeEvent({eventName: 'onPrintMapPlan', callBackFunc: me.onPrintMapPlan, scope: me});
		me.appEventsController.removeEvent({eventName: 'onLoadMapPlan', callBackFunc: me.onLoadPlanCompleteMapPlan, scope: me});

        me.appEventsController.removeEvent({eventName: 'updateAllPlanHours', callBackFunc: me.updateAllPlanHours, scope: me});
		me.appEventsController.removeEvent({eventName: 'onViewCourseNotes', callBackFunc: me.onViewCourseNotes, scope: me});
        
		return me.callParent( arguments );
    },
});
