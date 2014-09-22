var ortolangDiffControllers = angular.module('ortolangDiffControllers', []);

ortolangDiffControllers.controller('searchCtrl', ['$routeParams',
    function($routeParams) {
        this.name = "searchCtrl";
        this.params = $routeParams;
    }]);

//ortolangDiffControllers.controller('WorkspaceElementsCtrl', ['$scope', '$http', '$routeParams', '$rootScope',
//    function ($scope, $http, $routeParams, $rootScope) {
//
//        $scope.urlBase = ortolangDiffApp.urlBase;
//        $scope.wsName = $routeParams.wsName;
//        $scope.rootName = $routeParams.rootName;
//        $scope.orderProp = ['type', 'name'];
//        $scope.newCollectionName = undefined;
//        $scope.newCollectionDescription = undefined;
//
//        var url = ortolangDiffApp.urlBase + '/rest/workspaces/' +
//            $scope.wsName + '/elements?root=' + $scope.rootName + '&path=' + $routeParams.elementPath;
//
//        function buildSelectedChildDeleteUrl() {
//            return ortolangDiffApp.urlBase + '/rest/workspaces/' + $scope.wsName + '/elements?path=' + $scope.element.path + '/' + $scope.selectedChildData.object.name;
//        }
//
//        function buildSelectedChildDownloadUrl() {
//            return ortolangDiffApp.urlBase + "/rest/workspaces/" + $scope.wsName + "/download?path=" + $scope.element.path + "/" + $scope.selectedChildData.object.name;
//        }
//
//        function contextMenu(clickEvent, sameChild) {
//            // If right click
//            if (clickEvent.button === 2) {
//                $scope.contextMenuStyle = {
//                    position: 'absolute',
//                    display: 'block',
//                    left: clickEvent.pageX + "px",
//                    // TODO: HACK fix dropdown offset because of navbar
//                    // top: clickEvent.pageY-51+"px"
//                    top: clickEvent.pageY + "px"
//                };
//                // If the context menu has already been build no need to do it again
//                if ($scope.contextMenu && sameChild) {
//                    return;
//                }
//                $scope.contextMenu = true;
//                $scope.menuItems = [];
//                if ($scope.selectedChild.type === "COLLECTION") {
//                    $scope.menuItems.push({text: "New Collection", icon: "plus", action: "newCollection"});
//                    $scope.menuItems.push({divider: true});
//                }
//                if ($scope.selectedChildData.object.stream) {
//                    $scope.menuItems.push({text: "Download", icon: "download", href: $scope.selectedChildData.object.downloadUrl});
//                }
//                $scope.menuItems.push({text: "Delete", icon: "trash", action: "delete"});
//            } else {
//                $scope.contextMenu = false;
//            }
//        }
//
//        function getElementData(refresh) {
//            $http.get(url).success(function (data) {
//                $scope.element = data;
//                // If we just refresh the data no need to build the breadcrumb again
//                if (!refresh) {
//                    var breadcrumbParts = [], tmp = '';
//                    angular.forEach(data.pathParts, function (key) {
//                        tmp += '/' + key;
//                        breadcrumbParts.push(tmp);
//                    });
//                    $scope.breadcrumbParts = breadcrumbParts;
//                    $scope.path = data.path.replace("/", "head/root/").split("/");
//                }
//            });
//        }
//
//        $http.defaults.headers.common.Authorization = 'Basic cm9vdDp0YWdhZGE1NA==';
//        getElementData(false);
//
//        $scope.clickChild = function (clickEvent, child) {
//            if ($scope.selectedChild === child) {
//                contextMenu(clickEvent, true);
//                return;
//            }
//            $scope.selectedChild = child;
//            // Get detailed info on the selected child
//            $http.get(ortolangDiffApp.urlBase + '/rest/objects/' + child.key).success(function (data) {
//                $scope.selectedChildData = data;
//                $scope.selectedChildData.object.downloadUrl = buildSelectedChildDownloadUrl();
//                contextMenu(clickEvent, false);
//            });
//        };
//
//        $scope.isSelected = function (item) {
//            return $scope.selectedChild === item;
//        };
//
//        function deselectChild() {
//            $scope.selectedChild = undefined;
//            $scope.selectedChildData = undefined;
//            $scope.contextMenu = false;
//        }
//
//        $scope.checkSelection = function (clickEvent) {
//            if (!($(clickEvent.target).parent('tr').length || $(clickEvent.target).parent('td').length ||
//                $(clickEvent.target).parents("#contextMenu").length)) {
//                deselectChild();
//            }
//        };
//
//        $scope.clickDelete = function () {
//            if (!$scope.selectedChild) {
//                return;
//            }
//            $http.delete(buildSelectedChildDeleteUrl()).success(function () {
//                $scope.selectedChildData = undefined;
//                getElementData(true);
//                deselectChild();
//            });
//        };
//
//        $scope.newCollectionModal = function () {
//            $('#newCollectionModal').modal('show');
//        };
//
//        $scope.addCollection = function () {
//            if ($scope.newCollectionName !== undefined) {
//                $("#newCollectionName").parentsUntil("form", ".form-group").removeClass("has-error");
//                var data = {
//                    path: $scope.element.path + "/" + ($scope.selectedChild &&
//                        $scope.selectedChild.type === "COLLECTION" ? $scope.selectedChild.name + "/" : "") +
//                        $scope.newCollectionName,
//                    type: "collection"
//                };
//                if ($scope.newCollectionDescription) {
//                    data.description = $scope.newCollectionDescription;
//                }
//                $http.put(ortolangDiffApp.urlBase + '/rest/workspaces/' + $routeParams.wsName + '/elements', data).success(function () {
//                    getElementData(true);
//                    $('#newCollectionModal').modal('hide');
//                    $scope.newCollectionName = undefined;
//                    $scope.newCollectionDescription = undefined;
//                    $scope.contextMenu = false;
//                });
//            } else {
//                $("#newCollectionName").parentsUntil("form", ".form-group").addClass("has-error");
//            }
//        };
//
//        $rootScope.$on('completeItemUpload', function () {
//            getElementData(true);
//        });
//
//    }]);
//
//ortolangDiffControllers.controller('DetailCtrl', ['$scope', '$routeParams',
//    function ($scope, $routeParams) {
//        $scope.elementPath = $routeParams.elementPath;
//    }]);