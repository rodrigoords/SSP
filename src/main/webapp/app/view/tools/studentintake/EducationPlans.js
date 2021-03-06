/*
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
Ext.define('Ssp.view.tools.studentintake.EducationPlans', {
	extend: 'Ext.form.Panel',
	alias: 'widget.studentintakeeducationplans',
	id : 'StudentIntakeEducationPlans',   
    mixins: [ 'Deft.mixin.Injectable',
              'Deft.mixin.Controllable'],
    controller: 'Ssp.controller.tool.studentintake.EducationPlansViewController',
    inject: {
    	formUtils: 'formRendererUtils',
        studentStatusesStore: 'studentStatusesStore',
        textStore:'sspTextStore'
    },
	width: '100%',
    height: '100%',
	minHeight: 1000,
	minWidth: 600,
	style: 'padding: 0px 5px 5px 10px',
	
    initComponent: function() {	
    	var me=this;
		Ext.apply(this, 
				{
					autoScroll: true,
				    layout: {
				    	type: 'vbox',
				    	align: 'stretch'
				    },
				    border: 0,
				    defaults: {
				        anchor: '100%'
				    },
				    fieldDefaults: {
				        msgTarget: 'side',
				        labelAlign: 'left',
				        labelWidth: 225
				    },
				    defaultType: 'displayfield',
				    items: [{
				            xtype: 'container',
							border: 0,
							padding: 10,
				            title: '',
				            defaultType: 'textfield',
				            defaults: {
				                anchor: '95%'
				            },
				       items: [{
				        xtype: 'combobox',
				        name: 'studentStatusId',
				        itemId: 'studentStatusCombo',
				        fieldLabel: me.textStore.getValueByCode('intake.tab3.label.student-status', 'Student Status'),
				        emptyText: me.textStore.getValueByCode('intake.tab3.empty-text.student-status', 'Select One'),
				        store: me.studentStatusesStore,
				        valueField: 'id',
				        displayField: 'name',
				        mode: 'local',
				        typeAhead: true,
				        queryMode: 'local',
				        allowBlank: true,
						forceSelection: true,
						listeners:{
							'change': function() {
								if (this.getValue() === null) {
    								this.reset();
  								}
							}
						}
					},{
				        xtype: 'radiogroup',
				        fieldLabel: me.textStore.getValueByCode('intake.tab3.label.parents-college', 'Have your parents obtained a college degree?'),
				        columns: 1,
				        itemId: "collegeDegreeForParents",
				        items: [
				            {boxLabel: "Yes", itemId: "collegeDegreeForParentsCheckOn", name: "collegeDegreeForParents", inputValue:true},
				            {boxLabel: "No", itemId: "collegeDegreeForParentsCheckOff", name: "collegeDegreeForParents", inputValue:false}]
				    },{
				        xtype: "radiogroup",
				        fieldLabel: me.textStore.getValueByCode('intake.tab3.label.special-needs', 'Special needs or require special accomodation?'),
				        columns: 1,
				        itemId: "specialNeeds",
				        items: [
				            {boxLabel: "Yes", itemId: "specialNeedsCheckOn", name: "specialNeeds", inputValue:"y"},
				            {boxLabel: "No", itemId: "specialNeedsCheckOff", name: "specialNeeds", inputValue:"n"}]
				    },{
				        xtype: 'radiogroup',
				        fieldLabel: me.textStore.getValueByCode('intake.tab3.label.typical-grade', 'What grade did you typically earn at your highest level of education?'),
				        columns: 1,
				        items: [
				            {boxLabel: 'A', name: 'gradeTypicallyEarned', inputValue: "1"},
				            {boxLabel: 'A-B', name: 'gradeTypicallyEarned', inputValue: "2"},
				            {boxLabel: 'B', name: 'gradeTypicallyEarned', inputValue: "3"},
				            {boxLabel: 'B-C', name: 'gradeTypicallyEarned', inputValue: "4"},
				            {boxLabel: 'C', name: 'gradeTypicallyEarned', inputValue: "5"},
				            {boxLabel: 'C-D', name: 'gradeTypicallyEarned', inputValue: "6"},
				            {boxLabel: 'D', name: 'gradeTypicallyEarned', inputValue: "7"},
				            {boxLabel: 'D-F', name: 'gradeTypicallyEarned', inputValue: "8"},
				            {boxLabel: 'F', name: 'gradeTypicallyEarned', inputValue: "9"}
				    		]
				    }]
				    }]
				});
		
		return me.callParent(arguments);
	}	
});