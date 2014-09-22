ortolangDiffControllers.controller('MetadataUploadCtrl', ['$scope', '$http', '$routeParams', '$rootScope', 'FileUploader',
    function ($scope, $http, $routeParams, $rootScope, FileUploader) {
        "use strict";

        var url = ortolangDiffApp.urlBase + '/rest/workspaces/' + $routeParams.wsName + '/elements';

        var metadataUploader = $scope.metadataUploader = new FileUploader({
            url: url,
            alias: 'stream',
            autoUpload: true,
            removeAfterUpload: false,
            headers: {
                'Authorization': 'Basic cm9vdDp0YWdhZGE1NA=='
            },
            routeParams: $routeParams
        });

        metadataUploader.onAfterAddingFile = function (fileItem) {
            fileItem.formData = [{ path: this.routeParams.elementPath + ($rootScope.getSelectedChild() ? '/' + $rootScope.getSelectedChild().name : "")},
                {type: "metadata"},
                {name: fileItem.file.name}];
            console.info('onAfterAddingFile', fileItem);
            console.info('formData', fileItem.formData[0], fileItem.formData[1]);
        };

        metadataUploader.onCompleteItem = function (fileItem, response, status, headers) {
            console.info('onCompleteItem', fileItem, response, status, headers);
            $rootScope.$emit('completeMetadataUpload');
        };

    }]);