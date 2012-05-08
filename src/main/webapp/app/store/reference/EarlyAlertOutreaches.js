Ext.define('Ssp.store.reference.EarlyAlertOutreaches', {
    extend: 'Ssp.store.reference.AbstractReferences',
    model: 'Ssp.model.reference.EarlyAlertOutreach',
    constructor: function(){
    	this.callParent(arguments);
    	Ext.apply(this.getProxy(),{url: this.getProxy().url + 'earlyAlertOutreach/'});
    }
});