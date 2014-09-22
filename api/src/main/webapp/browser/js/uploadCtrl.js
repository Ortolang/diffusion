ortolangDiffControllers.controller('UploadCtrl', ['$scope', '$http', '$routeParams', '$rootScope', 'FileUploader',
    function ($scope, $http, $routeParams, $rootScope, FileUploader) {
        "use strict";

        var url = ortolangDiffApp.urlBase + '/rest/workspaces/' + $routeParams.wsName + '/elements';

        var uploader = $scope.uploader = new FileUploader({
            url: url,
            alias: 'stream',
            autoUpload: false,
            removeAfterUpload: false,
            headers: {
                'Authorization': 'Basic cm9vdDp0YWdhZGE1NA=='
            },
            routeParams: $routeParams
        });

        // FILTERS

        // uploader.filters.push({
        //     name: 'customFilter',
        //     fn: function(item /*{File|FileLikeObject}*/, options) {
        //         return this.queue.length < 10;
        //     }
        // });

        // CALLBACKS

        uploader.onWhenAddingFileFailed = function (item, filter, options) {
            console.info('onWhenAddingFileFailed', item, filter, options);
        };
        uploader.onAfterAddingFile = function (fileItem) {
            fileItem.formData = [{ path: this.routeParams.elementPath + '/' + fileItem.file.name}, {type: "object"}];
            console.info('onAfterAddingFile', fileItem);
            console.info('formData', fileItem.formData[0], fileItem.formData[1]);
        };
        uploader.onAfterAddingAll = function (addedFileItems) {
            console.info('onAfterAddingAll', addedFileItems);
        };
        uploader.onBeforeUploadItem = function (item) {
            console.info('onBeforeUploadItem', item);
        };
        uploader.onProgressItem = function (fileItem, progress) {
            console.info('onProgressItem', fileItem, progress);
        };
        uploader.onProgressAll = function (progress) {
            console.info('onProgressAll', progress);
        };
        uploader.onSuccessItem = function (fileItem, response, status, headers) {
            console.info('onSuccessItem', fileItem, response, status, headers);
        };
        uploader.onErrorItem = function (fileItem, response, status, headers) {
            console.info('onErrorItem', fileItem, response, status, headers);
        };
        uploader.onCancelItem = function (fileItem, response, status, headers) {
            console.info('onCancelItem', fileItem, response, status, headers);
        };
        uploader.onCompleteItem = function (fileItem, response, status, headers) {
            console.info('onCompleteItem', fileItem, response, status, headers);
            $rootScope.$emit('completeItemUpload');
        };
        uploader.onCompleteAll = function () {
            console.info('onCompleteAll');
        };

        console.info('uploader', uploader);
    }]);