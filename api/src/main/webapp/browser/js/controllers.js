var ortolangDiffControllers = angular.module('ortolangDiffControllers', []);

ortolangDiffControllers.controller('WorkspaceElementsCtrl', ['$scope', '$http', '$routeParams', '$rootScope', '$filter',
    function ($scope, $http, $routeParams, $rootScope, $filter) {

        $scope.urlBase = ortolangDiffApp.urlBase;
        $scope.wsName = $routeParams.wsName;
        $scope.rootName = $routeParams.rootName;
        $scope.orderProp = ['type', 'name'];
        $scope.dateFormat = 'medium';
        $scope.reverse = false;
        $scope.newCollectionName = undefined;
        $scope.newCollectionDescription = undefined;
        $scope.code = undefined;

        var url = ortolangDiffApp.urlBase + '/rest/workspaces/' +
            $scope.wsName + '/elements?root=' + $scope.rootName + '&path=' + $routeParams.elementPath;

        function buildSelectedChildDeleteUrl() {
            return ortolangDiffApp.urlBase + '/rest/workspaces/' + $scope.wsName + '/elements?path=' + $scope.element.path + '/' + $scope.selectedChildData.object.name;
        }

        function buildSelectedChildDownloadUrl() {
            return ortolangDiffApp.urlBase + "/rest/workspaces/" + $scope.wsName + "/download?path=" + $scope.element.path + "/" + $scope.selectedChildData.object.name;
        }

        function contextMenu(clickEvent, sameChild) {
            // If right click
            if (clickEvent.button === 2) {
                $scope.contextMenuStyle = {
                    position: 'absolute',
                    display: 'block',
                    left: clickEvent.pageX + "px",
                    // TODO: HACK fix dropdown offset because of navbar
                    // top: clickEvent.pageY-51+"px"
                    top: clickEvent.pageY + "px"
                };
                // If the context menu has already been build no need to do it again
                if ($scope.contextMenu && sameChild) {
                    return;
                }
                $scope.contextMenu = true;
                $scope.menuItems = [];
                if ($scope.selectedChild.type === "collection") {
                    $scope.menuItems.push({text: "New Collection", icon: "plus", action: "newCollection"});
                    $scope.menuItems.push({divider: true});
                }
                if ($scope.selectedChildData.object.stream) {
                    $scope.menuItems.push({text: "Download", icon: "download", href: $scope.selectedChildData.object.downloadUrl});
                }
                $scope.menuItems.push({text: "Delete", icon: "trash", action: "delete"});
            } else {
                $scope.contextMenu = false;
            }
        }

        function getElementData(refresh) {
            $http.get(url).success(function (data) {
                $scope.element = data;
                // If we just refresh the data no need to build the breadcrumb again
                if (!refresh) {
                    var breadcrumbParts = [], tmp = '';
                    angular.forEach(data.pathParts, function (key) {
                        tmp += '/' + key;
                        breadcrumbParts.push(tmp);
                    });
                    $scope.breadcrumbParts = breadcrumbParts;
                    $scope.path = data.path.replace("/", "head/root/").split("/");
                }
            });
        }

        function getChildData(refresh, clickEvent) {
            clickEvent = clickEvent || undefined;
            $http.get(ortolangDiffApp.urlBase + '/rest/objects/' + $scope.selectedChild.key).success(function (data) {
                $scope.selectedChildData = data;
                if (!refresh) {
                    $scope.selectedChildData.object.downloadUrl = buildSelectedChildDownloadUrl();
                    contextMenu(clickEvent, false);
                }
            });
        }

        // root
        $http.defaults.headers.common.Authorization = 'Basic cm9vdDp0YWdhZGE1NA==';
        // user 1
//        $http.defaults.headers.common.Authorization = 'Basic dXNlcjE6dGFnYWRh';
        getElementData(false);

        $scope.clickChild = function (clickEvent, child) {
            if ($scope.selectedChild === child) {
                contextMenu(clickEvent, true);
                return;
            }
            $scope.selectedChild = child;
            // Get detailed info on the selected child
            getChildData(false, clickEvent);
        };

        $scope.isSelected = function (item) {
            return $scope.selectedChild === item;
        };

        function deselectChild() {
            $scope.selectedChild = undefined;
            $scope.selectedChildData = undefined;
            $scope.contextMenu = false;
        }

        $scope.checkSelection = function (clickEvent) {
            if (!($(clickEvent.target).parent('tr').length || $(clickEvent.target).parent('td').length ||
                $(clickEvent.target).parents("#contextMenu").length || $(clickEvent.target).parents(".btn-toolbar").length)) {
                deselectChild();
            }
        };

        $scope.clickDelete = function () {
            if (!$scope.selectedChild) {
                return;
            }
            $http.delete(buildSelectedChildDeleteUrl()).success(function () {
                $scope.selectedChildData = undefined;
                getElementData(true);
                deselectChild();
            });
        };

        $scope.newCollectionModal = function () {
            $('#newCollectionModal').modal('show');
        };

        $scope.addCollection = function () {
            if ($scope.newCollectionName !== undefined) {
                $("#newCollectionName").parentsUntil("form", ".form-group").removeClass("has-error");
                var data = {
                    path: $scope.element.path + "/" + ($scope.selectedChild &&
                        $scope.selectedChild.type === "collection" ? $scope.selectedChild.name + "/" : "") +
                        $scope.newCollectionName,
                    type: "collection"
                };
                if ($scope.newCollectionDescription) {
                    data.description = $scope.newCollectionDescription;
                }
                $http.put(ortolangDiffApp.urlBase + '/rest/workspaces/' + $routeParams.wsName + '/elements', data).success(function () {
                    getElementData(true);
                    $('#newCollectionModal').modal('hide');
                    $scope.newCollectionName = undefined;
                    $scope.newCollectionDescription = undefined;
                    $scope.contextMenu = false;
                });
            } else {
                $("#newCollectionName").parentsUntil("form", ".form-group").addClass("has-error");
            }
        };

        $rootScope.$on('completeItemUpload', function () {
            getElementData(true);
        });

        $rootScope.$on('completeMetadataUpload', function () {
            if ($scope.selectedChild) {
                getChildData(true);
            } else {
                getElementData(true);
            }
        });

        $('#sideTabs').find('a').click(function (e) {
            e.preventDefault();
        });

        $scope.loadMetadata = function (clickEvent, metadata) {
            clickEvent.preventDefault();
            if ($scope.selectedMetadata === metadata) {
                $scope.selectedMetadata = undefined;
                $scope.code = undefined;
                $(clickEvent.target).removeClass("active");
                return;
            }
            if ($scope.selectedMetadata) {
                $(".metadata-" + $scope.selectedMetadata.key).removeClass("active");
            }
            $(clickEvent.target).addClass("active");
            $scope.selectedMetadata = metadata;
            $http.get(ortolangDiffApp.urlBase + '/rest/objects/' + metadata.key + '/download').success(function (data) {
                $scope.code = data;
            });
        };

        $scope.order = function (predicate, reverse) {
            if (predicate !== $scope.orderProp) {
                reverse = false;
            }
            $scope.orderReverse = reverse === "toggle" ? !$scope.orderReverse : reverse;
            $scope.orderProp = predicate;
        };

        $scope.filterChildren = function (query) {
            return function (child) {
                var re = new RegExp(query, 'gi');
                return child.name.match(re) || ($filter('date')(child.modification, $scope.dateFormat)).match(re);
            };
        };

        $scope.toggleTab = function (clickEvent) {
            clickEvent.preventDefault();
            $(clickEvent.target).tab('show');
        };

        $rootScope.getSelectedChild = function () {
            return $scope.selectedChild;
        };
    }]);

ortolangDiffControllers.controller('DetailCtrl', ['$scope', '$routeParams',
    function ($scope, $routeParams) {
        $scope.elementPath = $routeParams.elementPath;
    }]);