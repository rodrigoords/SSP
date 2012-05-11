Ext.define('Ssp.controller.admin.journal.AssociateTrackStepsAdminViewController', {
    extend: 'Deft.mvc.ViewController',
    mixins: [ 'Deft.mixin.Injectable' ],
    inject: {
    	apiProperties: 'apiProperties',
    	formUtils: 'formRendererUtils',
    	store: 'journalTracksStore',
    	treeStore: 'treeStore',
    	treeUtils: 'treeRendererUtils'
    },
    control: {
    	view: {
    		itemexpand: 'onItemExpand'
    	},
    	
        treeView: {
            selector: '.treeview',
            listeners: {
                beforedrop: 'onBeforeDrop'
            }
        }   	
    },
    
	init: function() {
		var me = this;
		var successFunc = function(response,view){
	    	var r = Ext.decode(response.responseText);
	    	var records = r.rows;
	    	if (records.length > 0)
	    	{
	    		var nodes = me.treeUtils.createNodesFromJson(records);
		    	me.treeStore.setRootNode({
		    	        text: 'root',
		    	        expanded: true,
		    	        children: nodes
		    	});
	    	}		
		};

		// clear the current items in the tree
    	me.treeStore.setRootNode({
	        text: 'root',
	        expanded: true,
	        children: []
	    });		
		
		this.apiProperties.makeRequest({
			url: this.apiProperties.createUrl('reference/journalTrack/'),
			method: 'GET',
			jsonData: '',
			successFunc: successFunc 
		});
    	
		return this.callParent(arguments);
    },
   
    onItemExpand: function(){
    	console.log('AssociateTrackStepsAdminViewController->onItemExpand');
    	// TODO: Retrieve related items and display as a subnode of the tree   	
    },

    onBeforeDrop: function(node, data, overModel, dropPosition, dropHandler, eOpts) {
    	dropHandler.wait=true;
        
    	/*
    	console.log(data.records[0].get('name'));
        console.log(overModel);
        console.log(dropPosition);
        console.log(dropHandler);
        console.log(eOpts);
        */

    	// handle drop on a folder
        if (!overModel.isLeaf() && dropPosition == 'append')
        {
        	console.log('stepId' + data.records[0].get('id'));
        	console.log('trackId' + overModel.data.id);
        	console.log("Make a call to add the step to the track.");
        }

        // handle drop inside a folder
        if (dropPosition=='before' || dropPosition=='after')
        	console.log("You can't do that. Drop it on a folder");
        
        dropHandler.cancelDrop;
        
        return 1;
    }	
});