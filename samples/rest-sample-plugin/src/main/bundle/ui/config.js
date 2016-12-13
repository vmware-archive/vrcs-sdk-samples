/*
 * Copyright © 2016 VMware, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Some files may be comprised of various open source software components, each of which
 * has its own license that is located in the source code of the respective component.
 */

/**
 * Render the config form with options for both input and output sections. Here we've also added a postRender callback
 * that gets fired once the form has completely rendered, to specify additional behaviors for the controls.
 */

/**
 * Import jQuery UI since it's required by typeahead
 */
VRCS.view.alpaca.importResource('lib/jquery-ui.js', 'lib/jquery-ui.css');

/**
 * Loose regex to check for variable binding.
 */
var variableBindingRegex = /^[$]\{(.*)\}/;

/**
 * The options for rendering "Input Parameters" (input) section of the config form. For more details about these options,
 * please refer to http://alpacajs.org/documentation.html
 */
var inputAlpaca = {
    schema: {
        /**
         * Specifying the title and add a help icon with link to http://vmware.com to the top right corner of the
         * screen.
         */
        title: VRCS.view.alpaca.createTitleWithIcon('Input Parameters', 'help', 'help.html'),
        type: 'object',
        properties: {
            endpoint: {
                title: 'Endpoint',
                required: true
            },
            method: {
                title: 'Method',
                required: true,
                dependencies: ['endpoint'],
                default: 'GET',
                enum: ['GET', 'POST', 'PUT', 'HEAD', 'OPTIONS', 'DELETE', 'TRACE']
            },
            path: {
                title: 'Relative Path',
                required: true,
                dependencies: ['endpoint']
            },
            headers: {
                title: 'Headers',
                type: 'array',
                dependencies: ['endpoint'],
                items: {
                    type: 'object',
                    properties: {
                        name: {
                            title: 'Key',
                            type: 'string'
                        },
                        value: {
                            title: 'Value',
                            type: 'string'
                        }
                    }
                }
            },
            body: {
                title: 'Body',
                type: 'string',
                dependencies: ['method']
            },
            expectedStatuses: {
                title: 'Expected Status Codes',
                type: 'string',
                dependencies: ['endpoint']
            },
            expectedResponse: {
                title: 'Expected Response Body',
                type: 'string',
                dependencies: ['endpoint']
            },
            poll: {
                title: 'Poll',
                type: 'boolean',
                dependencies: ['expectedResponse']
            },
            interval: {
                title: 'Interval',
                type: 'integer',
                required: true,
                minimum: 1,
                dependencies: ['poll']
            },
            timeout: {
                title: 'Timeout',
                type: 'integer',
                required: true,
                minimum: 1,
                dependencies: ['poll']
            },
            preview: {
                title: 'Preview',
                type: 'string',
                dependencies: ['method', 'path']
            }
        }
    },
    options: {
        fields: {
            endpoint: {
                type: 'select',
                noneLabel: '-- Select Endpoint --',
                removeDefaultNone: false,
                dataSource: function(setOptionFn) {
                    /**
                     * Provide the select with a list of endpoints that have been registered.
                     */
                    VRCS.io.queryEndpoints({
                        dataType: 'vrcs.rest-sample:RESTEndpoint',
                        complete: function(servers) {
                            var endpoints = servers.map(function(server) {
                                return {
                                    text: server.name,
                                    value: server.reference
                                };
                            });
                            setOptionFn(endpoints);
                        },
                        fail: function(error) {
                            VRCS.log.error(error);
                            VRCS.view.showMessage('Failed to retrieve REST Endpoint.');
                        }
                    });
                }
            },
            method: {
                type: 'select',
                removeDefaultNone: true,
                sort: false
            },
            path: {
                placeholder: 'eg: /rest-api/resource',
                validator: function(callback) {
                    var value = this.getValue();

                    var pathRegex =
                        /^(?:\/\/(?:(?:[A-Za-z0-9\-._~!$&'()*+,;=:]|%[0-9A-Fa-f]{2})*@)?(?:\[(?:(?:(?:(?:[0-9A-Fa-f]{1,4}:){6}|::(?:[0-9A-Fa-f]{1,4}:){5}|(?:[0-9A-Fa-f]{1,4})?::(?:[0-9A-Fa-f]{1,4}:){4}|(?:(?:[0-9A-Fa-f]{1,4}:){0,1}[0-9A-Fa-f]{1,4})?::(?:[0-9A-Fa-f]{1,4}:){3}|(?:(?:[0-9A-Fa-f]{1,4}:){0,2}[0-9A-Fa-f]{1,4})?::(?:[0-9A-Fa-f]{1,4}:){2}|(?:(?:[0-9A-Fa-f]{1,4}:){0,3}[0-9A-Fa-f]{1,4})?::[0-9A-Fa-f]{1,4}:|(?:(?:[0-9A-Fa-f]{1,4}:){0,4}[0-9A-Fa-f]{1,4})?::)(?:[0-9A-Fa-f]{1,4}:[0-9A-Fa-f]{1,4}|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))|(?:(?:[0-9A-Fa-f]{1,4}:){0,5}[0-9A-Fa-f]{1,4})?::[0-9A-Fa-f]{1,4}|(?:(?:[0-9A-Fa-f]{1,4}:){0,6}[0-9A-Fa-f]{1,4})?::)|[Vv][0-9A-Fa-f]+\.[A-Za-z0-9\-._~!$&'()*+,;=:]+)\]|(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|(?:[A-Za-z0-9\-._~!$&'()*+,;=]|%[0-9A-Fa-f]{2})*)(?::[0-9]*)?(?:\/(?:[A-Za-z0-9\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})*)*|\/(?:(?:[A-Za-z0-9\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})+(?:\/(?:[A-Za-z0-9\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})*)*)?|(?:[A-Za-z0-9\-._~!$&'()*+,;=@]|%[0-9A-Fa-f]{2})+(?:\/(?:[A-Za-z0-9\-._~!$&'()*+,;=:@]|%[0-9A-Fa-f]{2})*)*|)(?:\?(?:[A-Za-z0-9\-._~!$&'()*+,;=:@\/?]|%[0-9A-Fa-f]{2})*)?(?:\#(?:[A-Za-z0-9\-._~!$&'()*+,;=:@\/?]|%[0-9A-Fa-f]{2})*)?$/;

                    if (pathRegex.test(value) || variableBindingRegex.test(value)) {
                        callback({
                            status: true
                        });
                    } else {
                        callback({
                            status: false,
                            message: 'Invalid relative URL'
                        });
                    }
                }
            },
            headers: {
                type: 'table',
                showActionsColumn: false,
                hideToolbar: false,
                hideToolbarWithChildren: false,
                items: {
                    fields: {
                        name: {
                            placeholder: 'Key'
                        },
                        value: {
                            placeholder: 'Value'
                        }
                    }
                },
                toolbar: {
                    actions: [{
                        label: 'Add Header',
                        action: 'add'
                    }, {
                        label: 'Remove Last Header',
                        action: 'removeLast',
                        iconClass: 'glyphicon glyphicon-minus-sign',
                        click: function(key, action, itemIndex) {
                            var value = this.getValue();
                            if (value.length > 0) {
                                value.pop();
                                this.setValue(value);
                            }
                        }
                    }]
                }
            },
            body: {
                type: 'editor',
                aceMode: 'ace/mode/plain_text',
                size: 50,
                dependencies: {
                    method: ['POST', 'PUT']
                }
            },
            expectedStatuses: {
                type: 'token',
                placeholder: 'Leave blank to accept all status codes',
                tokenfield: {
                    autocomplete: {
                        source: ['100', '101', '102', '200', '201', '202', '203', '204', '205', '206', '207',
                            '208', '226', '300', '301', '302', '303', '304', '305', '306', '307', '308',
                            '400', '401', '402', '403', '404', '405', '406', '407', '408', '409', '410',
                            '411', '412', '413', '414', '415', '416', '417', '421', '422', '423', '424',
                            '426', '428', '429', '431', '451', '500', '501', '502', '503', '504', '505',
                            '506', '507', '508', '510', '511'],
                        delay: 100
                    },
                    showAutocompleteOnFocus: true
                },
                validator: function(callback) {
                    var value = this.getValue();

                    var httpStatusRegex = /^([1-5][0-9][0-9](,\s?[1-5][0-9][0-9])*)?$/;

                    if (!value) {
                        callback({
                            status: true
                        });
                    } else if (httpStatusRegex.test(value)) {
                        // Avoid duplication on the status codes
                        var statusCodeArray = value.split(/,\s?/);
                        var statusCodeMap = {};

                        $.each(statusCodeArray, function(i, code) {
                            if (statusCodeMap.hasOwnProperty(code)) {
                                statusCodeMap[code]++;
                            } else {
                                statusCodeMap[code] = 1;
                            }
                        });

                        var hasDuplicates = false;
                        var duplicateCodes = [];
                        $.each(statusCodeMap, function(key, value) {
                            if (value > 1) {
                                hasDuplicates = true;
                                duplicateCodes.push(key);
                            }
                        });

                        if (hasDuplicates) {
                            callback({
                                status: false,
                                message: 'Duplicate status: ' + duplicateCodes.join(', ')
                            });
                        } else {
                            callback({
                                status: true
                            });
                        }

                    } else {
                        callback({
                            status: false,
                            message: 'Invalid http status code'
                        });
                    }
                }
            },
            expectedResponse: {
                placeholder: 'Regular Expression eg: ^[a-z0-9_-]{3,16}$',
                validator: function(callback) {
                    var value = this.getValue();
                    try {
                        var regExp = new RegExp(value);
                        callback({
                            status: true
                        });
                    } catch (error) {
                        callback({
                            status: false,
                            message: error.message
                        });
                    }
                }
            },
            poll: {
                type: 'checkbox',
                rightLabel: 'Repeat until the expected response body is received.'
            },
            interval: {
                inputType: 'number',
                dependencies: {
                    poll: true
                }
            },
            timeout: {
                inputType: 'number',
                dependencies: {
                    poll: true
                }
            },
            preview: {
                placeholder: 'Click for preview',
                type: 'textarea',
                readonly: true,
                propertyBinding: false,
                events: {
                    click: function() {
                        var me = this;

                        var inputValues = me.top().getValue().input;
                        var endpoint = inputValues && inputValues.endpoint;
                        var path = inputValues && inputValues.path;
                        var headers = inputValues && inputValues.headers;
                        var method = inputValues && inputValues.method;
                        var body = inputValues && inputValues.body;

                        if (variableBindingRegex.test(path) || variableBindingRegex.test(body)) {
                            me.setValue('Preview not available for input with variable binding.');
                            return;
                        }

                        VRCS.io.callTile({
                            tile: 'vrcs.rest-sample:RESTPreview',
                            inputParameters: {
                                endpoint: endpoint,
                                path: path,
                                headers: headers,
                                method: method,
                                body: body
                            },
                            complete: function(outputParameters) {
                                me.setValue(outputParameters.responsePreview);
                            },
                            fail: function(error) {
                                VRCS.log.error(error);
                                VRCS.view.showMessage('Failed to get response preview');
                                me.setValue('Failed to get response preview with the error below.\n' + error[0].message);
                            }
                        });
                    }
                }
            }
        }
    }
};

/**
 * The options for rendering "Output Parameters" section of the config form. For more details about these options,
 * please refer to http://alpacajs.org/documentation.html
 */
var outputAlpaca = {
    schema: {
        title: 'Output Parameters',
        type: 'object'
    },
    options: {
        type: 'vrcs-outputtable',
        appendStatus: true
    }
};

/**
 * Show the config form with options for both input and output sections. Here we've also added a postRender callback
 * that gets fired once the form has completely rendered, to specify additional behaviors for the controls.
 */
VRCS.view.alpaca.showForm({
    alpacaForm: {
        schema: {
            type: 'object',
            properties: {
                input: inputAlpaca.schema,
                output: outputAlpaca.schema
            }
        },
        options: {
            fields: {
                input: inputAlpaca.options,
                output: outputAlpaca.options
            }
        },
        postRender: function(control) {
            /**
             * Hide preview field in view mode
             */
            if (!VRCS.context.readOnly) {
                return;
            }

            var inputControl = control.childrenByPropertyId['input'];
            var previewField = inputControl.childrenByPropertyId['preview'];

            previewField.hide();
        }
    }
});
