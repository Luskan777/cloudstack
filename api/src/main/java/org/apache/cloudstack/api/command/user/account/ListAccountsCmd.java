// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.user.account;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiConstants.DomainDetails;
import org.apache.cloudstack.api.BaseListDomainResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;

@APICommand(name = "listAccounts", description = "Lists accounts and provides detailed account information for listed accounts", responseObject = AccountResponse.class, responseView = ResponseView.Restricted, entityType = {Account.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class ListAccountsCmd extends BaseListDomainResourcesCmd implements UserCmd {
    private static final String s_name = "listaccountsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT_TYPE,
               type = CommandType.INTEGER,
               description = "list accounts by account type. Valid account types are 1 (admin), 2 (domain-admin), and 0 (user).")
    private Integer accountType;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "list account by account ID")
    private Long id;

    @Parameter(name = ApiConstants.IS_CLEANUP_REQUIRED, type = CommandType.BOOLEAN, description = "list accounts by cleanuprequired attribute (values are true or false)")
    private Boolean cleanupRequired;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "list account by account name")
    private String searchName;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "list accounts by state. Valid states are enabled, disabled, and locked.")
    private String state;

    @Parameter(name = ApiConstants.DETAILS,
               type = CommandType.LIST,
               collectionType = CommandType.STRING,
               description = "comma separated list of account details requested, value can be a list of [ all, resource, min]")
    private List<String> viewDetails;

    @Parameter(name = ApiConstants.API_KEY_ACCESS, type = CommandType.STRING, description = "List accounts by the Api key access value", since = "4.20.1.0", authorized = {RoleType.Admin})
    private String apiKeyAccess;

    @Parameter(name = ApiConstants.SHOW_RESOURCE_ICON, type = CommandType.BOOLEAN,
            description = "flag to display the resource icon for accounts")
    private Boolean showIcon;

    @Parameter(name = ApiConstants.TAG, type = CommandType.STRING, description = "Tag for resource type to return usage", since = "4.20.0")
    private String tag;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Account.Type getAccountType() {
        return Account.Type.getFromValue(accountType);
    }

    public Long getId() {
        return id;
    }

    public Boolean isCleanupRequired() {
        return cleanupRequired;
    }

    public String getSearchName() {
        return searchName;
    }

    public String getState() {
        return state;
    }

    public EnumSet<DomainDetails> getDetails() throws InvalidParameterValueException {
        EnumSet<DomainDetails> dv;
        if (viewDetails == null || viewDetails.size() <= 0) {
            dv = EnumSet.of(DomainDetails.all);
        } else {
            try {
                ArrayList<DomainDetails> dc = new ArrayList<DomainDetails>();
                for (String detail : viewDetails) {
                    dc.add(DomainDetails.valueOf(detail));
                }
                dv = EnumSet.copyOf(dc);
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("The details parameter contains a non permitted value. The allowed values are " +
                    EnumSet.allOf(DomainDetails.class));
            }
        }
        return dv;
    }

    public String getApiKeyAccess() {
        return apiKeyAccess;
    }

    public boolean getShowIcon() {
        return showIcon != null ? showIcon : false;
    }

    public String getTag() {
        return tag;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        ListResponse<AccountResponse> response = _queryService.searchForAccounts(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
        updateAccountResponse(response.getResponses());
    }

    protected void updateAccountResponse(List<AccountResponse> response) {
        if (CollectionUtils.isEmpty(response)) {
            return;
        }
        _resourceLimitService.updateTaggedResourceLimitsAndCountsForAccounts(response, getTag());
        if (!getShowIcon()) {
            return;
        }
        for (AccountResponse accountResponse : response) {
            ResourceIcon resourceIcon = resourceIconManager.getByResourceTypeAndUuid(ResourceTag.ResourceObjectType.Account, accountResponse.getObjectId());
            if (resourceIcon == null) {
                continue;
            }
            ResourceIconResponse iconResponse = _responseGenerator.createResourceIconResponse(resourceIcon);
            accountResponse.setResourceIconResponse(iconResponse);
        }
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Account;
    }
}
