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
 * Show the endpoint config form with one single section whose properties and options are defined below.
 */

VRCS.view.alpaca.showForm({
    alpacaForm: {
        schema: {
            title: VRCS.view.alpaca.createTitleWithIcon('Endpoint Properties', 'help', 'http://vmware.com'),
            type: 'object',
            properties: {
                url: {
                    title: 'Url',
                    required: true
                },
                username: {
                    title: 'Username'
                },
                password: {
                    title: 'Password'
                }
            }
        },
        options: {
            fields: {
                url: {
                    type: 'url',
                    allowIntranet: true,
                    placeholder: 'eg: protocol://host:port/'
                },
                username: {
                    placeholder: 'username'
                },
                password: {
                    type: 'password',
                    placeholder: 'password'
                }
            }
        }
    }
});