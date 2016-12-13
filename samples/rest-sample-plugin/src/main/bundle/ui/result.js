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
 * The options for rendering "Input Parameters" section of the result form. For more details about these options,
 * please refer to http://alpacajs.org/documentation.html
 */
var inputAlpaca = {
    schema: {
        title: 'Input Parameters',
        type: 'object',
        properties: {
            endpoint: {
                title: 'Endpoint'
            },
            path: {
                title: 'Relative URL'
            },
            method: {
                title: 'Method',
                enum: ['GET', 'POST', 'PUT', 'HEAD', 'OPTIONS', 'DELETE', 'TRACE']
            },
            headers: {
                title: 'Headers',
                type: 'array',
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
                type: 'string'
            },
            expectedResponse : {
                title: 'Expected Response body',
                type: 'string'
            },
            poll: {
                title: 'Poll',
                type: 'boolean'
            },
            interval: {
                title: 'Interval',
                type: 'integer'
            },
            timeout: {
                title: 'Timeout',
                type: 'integer'
            }
        }
    },
    options: {
        fields: {
            endpoint: {
                type: 'text'
            },
            path: {
                type: 'text'
            },
            method: {
                type: 'text'
            },
            headers: {
                type: 'table'
            },
            body: {
                type: 'editor',
                dependencies: {
                    method: ['POST', 'PUT']
                }
            },
            expectedStatuses: {
                type: 'token'
            },
            expectedResponse : {
                type: 'text'
            },
            poll : {
                type: 'checkbox',
                rightLabel: 'Repeat until the expected response body is received.'
            },
            interval : {
                inputType: 'number'
            },
            timeout : {
                inputType: 'number'
            }
        }
    }
};

/**
 * The options for rendering "Output Parameters" section of the result form. For more details about these options,
 * please refer to http://alpacajs.org/documentation.html
 */
var outputAlpaca = {
    schema: {
        title: 'Output Parameters',
        type: 'array',
        properties: {
            name: {
                type: 'string',
                title: 'Name'
            },
            type: {
                type: 'string',
                title: 'Type'
            },
            value: {
                type: 'any',
                title: 'Value'
            }
        }
    },
    options: {
        type: 'vrcs-outputtable',
        items: {
            type: 'vrcs-dynamictablerow',
            fields: {
                name: {
                    view: 'vrcs-display'
                },
                type: {
                    type: 'hidden'
                },
                value: {
                    type: 'string',
                    setup: function() {
                        /**
                         * "this" will be tablerow
                         * return the schema and option dynamically
                         */
                        var data = this.data;

                        if (data.name === 'responseHeaders' || data.name === 'responseBody') {
                            return {
                                options: {
                                    type: 'textarea'
                                }
                            }
                        }

                        return {
                            options: {
                                type: 'text'
                            }
                        };
                    }
                }
            }
        }
    }
};

/**
 * Show the result form with options for both input and output sections. Here we've also added a postRender callback
 * that gets fired once the form has completely rendered, to specify additional behaviors for the controls.
 *
 * NOTE: Result form is read only and does not take any inputs.
 */
VRCS.view.alpaca.showForm({
    alpacaForm: {
        data: {
            input: {
                endpoint: VRCS.context.task.inputParameters.values.endpoint.displayName
            }
        },
        schema: {
            type: 'object',
            properties: {
                output: outputAlpaca.schema,
                input: inputAlpaca.schema
            }
        },
        options: {
            fields: {
                output: outputAlpaca.options,
                input: inputAlpaca.options
            }
        }
    }
});
