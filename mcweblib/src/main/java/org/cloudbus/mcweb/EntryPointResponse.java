package org.cloudbus.mcweb;

import org.cloudbus.mcweb.DataCentre;

public class EntryPointResponse {
	
	private String selectedCloudSiteCode;
	private String redirectAddress;
	private DataCentre definition;
	
	public EntryPointResponse(final String selectedCloudSiteCode, final String redirectAddress) {
		this.selectedCloudSiteCode = selectedCloudSiteCode;
		this.redirectAddress = redirectAddress;
	}
	
	public EntryPointResponse(final String name, final String loadBalancerAddress, final DataCentre definition) {
		this.definition = definition;
	}

	public String getSelectedCloudSiteCode() {
		return selectedCloudSiteCode;
	}
	
	public void setSelectedCloudSiteCode(String selectedCloudSiteCode) {
		this.selectedCloudSiteCode = selectedCloudSiteCode;
	}
	
	public String getRedirectAddress() {
		return redirectAddress;
	}
	
	public void setRedirectAddress(String redirectAddress) {
		this.redirectAddress = redirectAddress;
	}

	public DataCentre getDefinition() {
		return definition;
	}

	public void setDefinition(DataCentre definition) {
		this.definition = definition;
	}
}
