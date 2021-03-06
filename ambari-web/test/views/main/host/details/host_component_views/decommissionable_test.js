/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');
require('models/host_component');
require('views/main/host/details/host_component_view');
require('mixins');
require('mixins/main/host/details/host_components/decommissionable');

var hostComponentView;

describe('App.Decommissionable', function() {

  beforeEach(function() {
    hostComponentView = App.HostComponentView.create(App.Decommissionable, {
      startBlinking: function(){},
      doBlinking: function(){},
      getDesiredAdminState: function(){return $.ajax({});}
    });
  });

  describe('#componentTextStatus', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({componentTextStatus: 'status'}),
        hostComponent: null,
        isComponentRecommissionAvailable: false,
        isComponentDecommissioning: false,
        e: 'status',
        m: 'get content status'
      },
      {
        content: Em.Object.create({componentTextStatus: 'status'}),
        hostComponent: Em.Object.create({componentTextStatus: 'new_status'}),
        isComponentRecommissionAvailable: false,
        isComponentDecommissioning: false,
        e: 'new_status',
        m: 'get hostComponent status'
      },
      {
        content: Em.Object.create({componentTextStatus: 'status'}),
        hostComponent: Em.Object.create({componentTextStatus: 'new_status'}),
        isComponentRecommissionAvailable: false,
        isComponentDecommissioning: false,
        e: 'new_status',
        m: 'get hostComponent status'
      },
      {
        content: Em.Object.create({componentTextStatus: 'status'}),
        hostComponent: Em.Object.create({componentTextStatus: 'new_status'}),
        isComponentRecommissionAvailable: true,
        isComponentDecommissioning: true,
        e: Em.I18n.t('hosts.host.decommissioning'),
        m: 'get decommissioning status'
      },
      {
        content: Em.Object.create({componentTextStatus: 'status'}),
        hostComponent: Em.Object.create({componentTextStatus: 'new_status'}),
        isComponentRecommissionAvailable: true,
        isComponentDecommissioning: false,
        e: Em.I18n.t('hosts.host.decommissioned'),
        m: 'get decommissioned status'
      }
    ]);

    tests.forEach(function(test) {
      it(test.m, function() {
        hostComponentView = App.HostComponentView.create(App.Decommissionable, {
          startBlinking: function(){},
          doBlinking: function(){},
          getDesiredAdminState: function(){return $.ajax({});},
          content: test.content,
          hostComponent: test.hostComponent,
          isComponentRecommissionAvailable: test.isComponentRecommissionAvailable,
          isComponentDecommissioning: test.isComponentDecommissioning
        });
        expect(hostComponentView.get('componentTextStatus')).to.equal(test.e);
      });
    });

  });

  describe('#statusClass', function() {

    var tests = Em.A([
      {
        content: Em.Object.create({workStatus: App.HostComponentStatus.install_failed,passiveState: 'OFF'}),
        isComponentRecommissionAvailable: false,
        e: 'health-status-color-red icon-cog'
      },
      {
        content: Em.Object.create({workStatus: App.HostComponentStatus.installing, passiveState: 'OFF'}),
        isComponentRecommissionAvailable: false,
        e: 'health-status-color-blue icon-cog'
      },
      {
        content: Em.Object.create({workStatus: 'STARTED', passiveState: 'ON'}),
        isComponentRecommissionAvailable: false,
        e: 'health-status-started'
      },
      {
        content: Em.Object.create({workStatus: 'STARTED', passiveState: 'IMPLIED'}),
        isComponentRecommissionAvailable: false,
        e: 'health-status-started'
      },
      {
        content: Em.Object.create({workStatus: 'STARTED', passiveState: 'OFF'}),
        isComponentRecommissionAvailable: false,
        e: 'health-status-started'
      },
      {
        content: Em.Object.create({workStatus: 'STARTED', passiveState: 'OFF'}),
        isComponentRecommissionAvailable: true,
        e: 'health-status-DEAD-ORANGE'
      },
      {
        content: Em.Object.create({workStatus: 'STARTING', passiveState: 'OFF'}),
        isComponentRecommissionAvailable: true,
        e: 'health-status-DEAD-ORANGE'
      },
      {
        content: Em.Object.create({workStatus: 'INSTALLED', passiveState: 'OFF'}),
        isComponentRecommissionAvailable: true,
        e: 'health-status-DEAD-ORANGE'
      }

    ]);

    tests.forEach(function(test) {
      it(test.content.get('workStatus') + ' ' + test.content.get('passiveState') + ' ' + test.isComponentRecommissionAvailable?'true':'false', function() {
        hostComponentView = App.HostComponentView.create(App.Decommissionable,{
          startBlinking: function(){},
          doBlinking: function(){},
          getDesiredAdminState: function(){return $.ajax({});},
          isComponentRecommissionAvailable: test.isComponentRecommissionAvailable,
          content: test.content
        });
        expect(hostComponentView.get('statusClass')).to.equal(test.e);
      });
    });

  });

  describe('#isInProgress', function() {

    var tests = Em.A([
      {
        workStatus: App.HostComponentStatus.stopping,
        isDecommissioning: false,
        e: true
      },
      {
        workStatus: App.HostComponentStatus.starting,
        isDecommissioning: false,
        e: true
      },
      {
        workStatus: 'other_status',
        isDecommissioning: false,
        e: false
      },
      {
        workStatus: 'other_status',
        isDecommissioning: true,
        e: true
      }
    ]);

    tests.forEach(function(test) {
      it(test.workStatus + ' ' + test.isDecommissioning?'true':'false', function() {

        hostComponentView = App.HostComponentView.create(App.Decommissionable,{
          startBlinking: function(){},
          doBlinking: function(){},
          getDesiredAdminState: function(){return $.ajax({});},
          isDecommissioning: test.isDecommissioning,
          content: Em.Object.create({workStatus: test.workStatus})
        });

        expect(hostComponentView.get('isInProgress')).to.equal(test.e);
      });
    });

  });

});
