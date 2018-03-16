define(['angular','nflow-mgr/nflows/edit-nflow/module-name'], function (angular,moduleName) {

    var directive = function () {
        return {
            restrict: "EA",
            bindToController: {},
            controllerAs: 'vm',
            scope: {},
            templateUrl: 'js/nflow-mgr/nflows/edit-nflow/lineage/nflow-lineage.html',
            controller: "NflowLineageController",
            link: function ($scope, element, attrs, controller) {

            }

        };
    }

    var controller = function ($scope, $element, $http, $mdDialog, $timeout, AccessControlService, NflowService, RestUrlService, VisDataSet, Utils, BroadcastService, StateService) {

        this.model = NflowService.editNflowModel;

        /**
         * Reference to this controller
         * @type {controller}
         */
        var self = this;
        /**
         * The Array of  Node Objects
         * @type {Array}
         */
        var nodes = [];

        /**
         * the Array of  edges
         * This connects nodes together
         * @type {Array}
         */
        var edges = [];

        /**
         * map to determine if edge link is needed
         * @type {{}}
         */
        var edgeKeys = {};

        /**
         * Data that is used to for the network graph
         * @type {{nodes: VisDataSet, edges: VisDataSet}}
         */
        self.data = {nodes:null, edges:null};

        /**
         * Object holding a reference the node ids that have been processed and connected in the graph
         * @type {{}}
         */
        var processedNodes = {};

        /**
         * The Response data that comes back from the server before the network is built.
         * This is a map of the various nflows {@code nflowLineage.nflowMap} and datasources {@code self.nflowLineage.datasourceMap}
         * that can be used to assemble the graph
         */
        var nflowLineage = null;

        /**
         * flag to indicate when the graph is loading
         * @type {boolean}
         */
        self.loading = false;

        /**
         * Enum for modes
         *
         * @type {{SIMPLE: string, DETAILED: string}}
         */
        var graphModes = {SIMPLE:"SIMPLE",DETAILED:"DETAILED"};

        /**
         * Set the default mode to SIMPLE view
         * @type {string}
         */
        self.graphMode = graphModes.DETAILED;



        /**
         * The default text of the node selector
         * @type {{name: string, type: string, content: {displayProperties: null}}}
         */
        var SELECT_A_NODE = {name: 'Select a node', type: 'Nflow Lineage', content: {displayProperties: null}};

        /**
         * The selected Node
         * @type {{name: string, type: string, content: null}}
         */
        self.selectedNode = SELECT_A_NODE;

        BroadcastService.subscribe($scope, BroadcastConstants.CONTENT_WINDOW_RESIZED, self.redraw);

        /**
         *
         * @type {{height: string, width: string, edges: {smooth: {forceDirection: string}}, physics: {barnesHut: {springLength: number}, minVelocity: number}, layout: {hierarchical: {enabled:
         *     boolean}}, nodes: {shape: string, font: {align: string}}, groups: {nflow: {color: {background: string}, font: {color: string}}, datasource: {color: {background: string}, font: {color:
         *     string}}}, interaction: {hover: boolean}}}
         */
        self.options = {
            height: '100%',
            width: '100%',
            "edges": {
                "arrowStrikethrough": false,
                smooth: {
                    enabled: true,
                    type: "cubicBezier",
                    roundness: 0,
                    "forceDirection": "horizontal"
                },
                font: {align: 'horizontal'}
            },
            layout: {
                hierarchical: {
                    direction: "LR",
                    nodeSpacing:200,
                    sortMethod:'directed'
                }
            },
            nodes: {
                shape: 'box',
                font: {
                    align: 'center'
                }
            },
            groups: {
                nflow: {
                    shape: 'box',
                    font: {
                        align: 'center'
                    }
                },
                datasource: {
                    shape: 'box',
                    font: {
                        align: 'center'
                    }
                }
            },
            interaction: {
                hover: true, navigationButtons: true,
                keyboard: true
            }
        };


        var isDetailedGraph = function(){
            return self.graphMode == graphModes.DETAILED;
        }

        self.redraw = function () {
            if (isDetailedGraph()) {
                self.onDetailedView();
            }
            else {
                self.onSimpleView();
            }
        }

        var _draw = function () {
            nodes = [];
            edges = [];
            edgeKeys ={};
            processedDatasource = {};
            processedNodes = {};

            //turn on physics for layout
            self.options.physics.enabled = true;
            buildVisJsGraph(nflowLineage.nflow);
            setNodeData();
        }

        self.onDetailedView = function () {
            self.graphMode = graphModes.DETAILED;
            _draw();

        }
        self.onSimpleView = function(){
            self.graphMode = graphModes.SIMPLE;
            _draw();
        }

        var setNodeStyle = function (node, style) {
            if (style) {
                if ((style.shape == 'icon' || !style.shape) && style.icon && style.icon.code) {
                    node.shape = 'icon';
                    if (angular.isObject(style.icon)) {
                        node.icon = style.icon;
                    }
                }
                else if (style.shape) {
                    node.shape = style.shape;
                }

                if (style.color) {
                    node.color = style.color;
                }
                if (style.size) {
                    node.size = style.size;
                }
                if (style.font) {
                    node.font = style.font;
                }
                if (!node.font) {
                    node.font = {}
                }
                node.font.background = '#ffffff'
            }


        }

        /**
         *  Builds the datasource Node for the passed in {@code dsId}
         * @param nflowLineage
         * @param nflow
         * @param dsId
         * @param type
         */
        var buildDatasource = function (nflow, dsId, type) {
            if(dsId) {
                var processedDatasource = processedNodes[dsId];

                if (processedDatasource == undefined) {
                    var ds = nflowLineage.datasourceMap[dsId];
                    assignDatasourceProperties(ds);
                    //console.log('building datasource',ds.name)
                    if (isDetailedGraph()) {
                        var node = {id: dsId, label: datasourceNodeLabel(ds), title: datasourceNodeLabel(ds), group: "datasource"};
                        nodes.push(node);
                        processedNodes[node.id] = node;
                        var style = nflowLineage.styles[ds.datasourceType];
                        setNodeStyle(node, style);


                    }

                    //build subgraph of nflow relationships
                    if (ds.sourceForNflows) {
                        _.each(ds.sourceForNflows, function (nflowItem) {
                            var depNflow = nflowLineage.nflowMap[nflowItem.id];
                            if (depNflow.id != nflow.id) {
                                var edgeKey = dsId + depNflow.id;
                                var fromId = dsId;
                                var arrows = "to";
                                var label = 'source'
                                if (!isDetailedGraph()) {
                                    label = '';
                                   edgeKey = nflow.id + depNflow.id;
                                   var edgeKey2 =depNflow.id + nflow.id;
                                   fromId = nflow.id;
                                   var exists = edgeKeys[edgeKey];
                                    var exists2 = edgeKeys[edgeKey2];
                                    if(!exists && exists2){
                                        exists2.arrows = "to;from";
                                        edgeKey = edgeKey2;
                                    }
                                }
                                if (edgeKeys[edgeKey] == undefined) {
                                    var edge = {from: fromId, to: depNflow.id, arrows: 'to', label: label};
                                    edgeKeys[edgeKey] = edge;
                                    edges.push(edge);
                                }
                                buildVisJsGraph(depNflow);
                            }
                        });
                    }
                    if (ds.destinationForNflows) {
                        _.each(ds.destinationForNflows, function (nflowItem) {
                            var depNflow = nflowLineage.nflowMap[nflowItem.id];
                            if (depNflow.id != nflow.id) {
                                var edgeKey = dsId + depNflow.id;
                                var toId = dsId;
                                var label = 'destination'
                                if (!isDetailedGraph()) {
                                    label = ''
                                    edgeKey =  depNflow.id +nflow.id;
                                    toId = nflow.id;

                                    var edgeKey2 =nflow.id + depNflow.id;
                                    var exists = edgeKeys[edgeKey];
                                    var exists2 = edgeKeys[edgeKey2];
                                    if(!exists && exists2 ){
                                        exists2.arrows = "to;from";
                                        edgeKey = edgeKey2;
                                    }
                                }
                                if (edgeKeys[edgeKey] == undefined) {
                                    var edge = {from: depNflow.id, to: toId, arrows: 'to', label: label};
                                    edgeKeys[edgeKey] = edge;
                                    edges.push(edge);
                                }
                                buildVisJsGraph(depNflow);
                            }
                        });
                    }

                }
                if (isDetailedGraph()) {
                    if (type == 'source') {
                        var edgeKey = dsId + nflow.id;
                        var fromId = dsId;
                        if (edgeKeys[edgeKey] == undefined) {
                            var edge = {from: fromId, to: nflow.id, arrows: 'to', label: 'source'};
                            edgeKeys[edgeKey] = edge;
                            edges.push(edge);
                        }
                    }
                    else {
                        var edgeKey = dsId + nflow.id;
                        var toId = dsId;
                        if (edgeKeys[edgeKey] == undefined) {
                            var edge = {from: nflow.id, to: toId, arrows: 'to', label: 'destination'};
                            edgeKeys[edgeKey] = edge;
                            edges.push(edge);
                        }
                    }

                }
            }
        }

        /**
         * Builds the Graph of Datasources as they are related to the incoming {@code nflow}
         * @param nflow
         */
        var buildDataSourceGraph = function (nflow) {
            if (nflow.sources) {
                _.each(nflow.sources, function (src) {
                    buildDatasource(nflow, src.datasourceId, 'source');
                });
            }
                if (nflow.destinations) {
                    _.each(nflow.destinations, function (dest) {
                        buildDatasource(nflow, dest.datasourceId, 'destination');
                    })
                }

        }

        /**
         * Assigns some of the fields on the datasource to the properties attribute
         * @param ds
         */
        var assignDatasourceProperties = function(ds){
            var keysToOmit = ['@type', 'id','name','encrypted','compressed','destinationForNflows','sourceForNflows'];
            var props = _.omit(ds, keysToOmit);
            props = _.pick(props, function (value, key) {
                return !_.isObject(value);
            });
            ds.displayProperties = props
            cleanProperties(ds);
            //add in any properties of its own
            angular.extend(ds.displayProperties, ds.properties);

        }

        var cleanProperties = function (item) {
            if (item.properties) {
                var updatedProps = _.omit(item.properties, function (val, key) {
                    return key.indexOf("jcr:") == 0 || key == "tba:properties" || key == 'tba:processGroupId';
                });
                item.properties = updatedProps
            }
        }

        /**
         * Get the label for the Datasource node
         * @param ds
         * @returns {string}
         */
        var datasourceNodeLabel = function(ds){
            var label = "";
            if (angular.isString(ds.datasourceType)) {
                label = Utils.endsWith(ds.datasourceType.toLowerCase(), "datasource") ? ds.datasourceType.substring(0, ds.datasourceType.toLowerCase().lastIndexOf("datasource")) : ds.datasourceType;
            } else if (angular.isString(ds.type)) {
                label = ds.type;
            } else {
                label = Utils.endsWith(ds["@type"].toLowerCase(), "datasource") ? ds["@type"].substring(0, ds["@type"].toLowerCase().lastIndexOf("datasource")) : ds["@type"];
            }

            label += "\n" + ds.name;
            return label;
        };

        /**
         * Get the label for the Nflow Node
         * @param nflow
         * @returns {string}
         */
        var nflowNodeLabel = function(nflow){
            var label = nflow.displayName;
            return label;
        }

        /**
         * Create the Nflow Node
         * @param nflow
         * @returns {{id: *, label: string, title: string, group: string}}
         */
        var nflowNode = function(nflow){

            var node = {id: nflow.id, label: nflowNodeLabel(nflow), title:nflowNodeLabel(nflow), group: "nflow"};
            var style = nflowLineage.styles['nflow'];
            setNodeStyle(node, style);
            if(nflow.id == self.model.id){
                var style = nflowLineage.styles['currentNflow'];
                setNodeStyle(node, style);
            }
            cleanProperties(nflow);
            nflow.displayProperties = {};
            //add in any properties of its own
            angular.extend(nflow.displayProperties, nflow.properties);

            return node;
        }

        /**
         * Build the Lineage Graph
         * @param nflow
         */
        var buildVisJsGraph = function (nflow) {

            var node = processedNodes[nflow.id] == undefined ? nflowNode(nflow) : processedNodes[nflow.id];
          if(processedNodes[node.id] == undefined) {
            //  console.log('building nflow',nflow.systemName)
              processedNodes[node.id] = node;
              nodes.push(node);
              buildDataSourceGraph(nflow);

              //walk the graph both ways (up and down)
              if (nflow.dependentNflowIds) {
                  _.each(nflow.dependentNflowIds, function (depNflowId) {
                      //get it from the map
                      var depNflow = nflowLineage.nflowMap[depNflowId];
                      if (processedNodes[depNflow.id] == undefined) {
                          buildVisJsGraph(depNflow)
                      }
                      var edgeKey = depNflow.id + nflow.id;
                      if (edgeKeys[edgeKey] == undefined) {
                          var edge = {from: depNflow.id, to: nflow.id, arrows: 'from', label: 'depends on'};
                          edgeKeys[edgeKey] = edge;
                          edges.push(edge);
                      }
                  });

              }
              if (nflow.usedByNflowIds) {
                  _.each(nflow.usedByNflowIds, function (usedByNflowId) {
                      //get it from the map
                      var usedByNflow = nflowLineage.nflowMap[usedByNflowId];
                      if (processedNodes[usedByNflow.id] == undefined) {
                          buildVisJsGraph(usedByNflow);
                      }
                      var edgeKey = nflow.id + usedByNflow.id;
                      if (edgeKeys[edgeKey] == undefined) {
                          // edgeKeys[edgeKey] = edgeKey;
                          //taken care of from the other side
                          //   edges.push({from: nflow.id, to: usedByNflow.id});
                      }

                  });
              }
          }

        }

/*
        self.clusterByDatasource = function() {

            var clusterOptionsByData = {
                joinCondition:function(childOptions) {
                    return childOptions.datasource == 1;
                },
                clusterNodeProperties: {id:'cidCluster', borderWidth:3, shape:'database'}
            };
            network.cluster(clusterOptionsByData);
        }
        */

        /**
         * Get the Nflow Lineage Graph and build the network
         * @param nflowId
         */
        var getNflowLineage = function(nflowId) {
            self.loading = true;

            $http.get(RestUrlService.NFLOW_LINEAGE_URL(nflowId)).then(function(response){
                nflowLineage = response.data;
                self.loading = false;
                self.redraw();


            });
        }
        /**
         * attach the nodes to the data to the graph
         */
        var setNodeData = function(){
            var visNodes = new VisDataSet(nodes);
            var visEdges = new VisDataSet(edges);
            self.data = {nodes:visNodes, edges:visEdges};
        }

        /**
         * Called when a Node is selected
         * @param item
         */
        self.changed = false;

        var onSelect = function (item) {
            if (item && item.nodes && item.nodes[0]) {
                self.changed = true;
                var firstItem = item.nodes[0];
                var nflow = nflowLineage.nflowMap[firstItem];
                if(nflow){
                    self.selectedNode.name = nflow.displayName;
                    self.selectedNode.type = 'NFLOW';
                    self.selectedNode.content=nflow;
                }
                else {
                    var ds = nflowLineage.datasourceMap[firstItem];
                    self.selectedNode.name = ds.name;
                    self.selectedNode.type = 'DATASOURCE';
                    self.selectedNode.content=ds;
                }

            }
            else {
                self.selectedNode = SELECT_A_NODE;
            }
            $scope.$apply()
            //console.log(;self.selectedNode);
            //force angular to refresh selection
            angular.element('#hiddenSelectedNode').html(self.selectedNode.name)
        };

        self.navigateToNflow = function () {
            if (self.selectedNode.type == 'NFLOW' && self.selectedNode.content) {
                StateService.NflowManager().Nflow().navigateToNflowDetails(self.selectedNode.content.id, 2);
            }

        }

        /**
         * Called when the graph is loaded
         * @param network
         */
        var onLoad = function(network){
            self.network = network;
           // stabilizationIterationsDone();
        }
        var stabilized = function(){
            stabilizationIterationsDone();
        }

        var stabilizationIterationsDone = function () {
            self.options.physics.enabled = false;
            if (self.network) {
                self.network.setOptions({
                    physics: {enabled: false, stabilization: false},
                    interaction: {dragNodes: true},
                    layout: {
                        hierarchical: {
                            enabled: false
                        }
                    }
                });
            }
        }


        self.events = {
            onload: onLoad,
            selectNode: onSelect,
            stabilized:stabilized
          //  stabilizationIterationsDone: stabilizationIterationsDone
        };

        /**
         * Load the Graph
         */
        getNflowLineage(self.model.id);
    };




    angular.module(moduleName).controller('NflowLineageController', ["$scope","$element","$http","$mdDialog","$timeout","AccessControlService","NflowService","RestUrlService","VisDataSet","Utils","BroadcastService","StateService",controller]);

    angular.module(moduleName)
        .directive('onescorpinNflowLineage', directive);

});

