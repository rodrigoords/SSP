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
Ext.define('Ssp.view.admin.forms.shg.EditSelfHelpGuideEditChallenges',{
	extend: 'Ext.form.Panel',
	alias : 'widget.editselfhelpguideeditchallenges',
    mixins: [ 'Deft.mixin.Injectable',
              'Deft.mixin.Controllable'],
    controller: 'Ssp.controller.admin.shg.SelfHelpGuideEditChallengesAdminController',
	title: 'Manage Challenges',
	height: '100%',
	width: '100%',
	layout: {
        type: 'hbox',
        align: 'stretch'
    },
    collapsible: true,
    initComponent: function(){
		Ext.apply(this,{
	          items: [
	                  {
	                  	xtype: 'editselfhelpguidechallenges', 
	                  	flex: 1
	                  },{
	                  	xtype: 'editselfhelpguideavailablechallengesadmin', 
	                  	flex: 1
	                  }
	                 ]});
    	return this.callParent(arguments);
    }
});

 