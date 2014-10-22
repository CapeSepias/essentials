/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
    "use strict";

    angular.module('hippo.essentials').controller('galleryPluginCtrl', function ($scope, $sce, $log, $rootScope, $http) {

        var endpoint = $rootScope.REST.dynamic + "galleryplugin";
        $scope.imageSets = [];
        $scope.selectedImageSet = null;
        $scope.imageVariantName = null;
        $scope.selectedImageModel = null;
        $scope.updateExisting = true;

        $scope.addImageSet = function () {
            var payload = Essentials.addPayloadData("imageSetPrefix", $scope.imageSetPrefix, null);
            Essentials.addPayloadData("imageSetName", $scope.imageSetName, payload);
            Essentials.addPayloadData("updateExisting", $scope.updateExisting, payload);
            $http.post(endpoint + "/create", payload).success(function () {
                loadImageSets($scope.imageSetPrefix + ':' + $scope.imageSetName);
                $scope.imageSetName = null;
            });
        };

        $scope.addImageVariant = function () {
            var exists = false;
            angular.forEach($scope.selectedImageSet.models, function (model) {
                if (model.name == $scope.imageVariantName) {
                    exists = true;
                }
            });
            if (exists) {
                displayError("Image variant with name '" + $scope.imageVariantName + "' already exists");
                return;
            }
            var payload = Essentials.addPayloadData("imageVariantName", $scope.imageVariantName, null);
            Essentials.addPayloadData("selectedImageSet", $scope.selectedImageSet.name, payload);
            $http.post(endpoint + "/addvariant", payload).success(function (data) {
                loadImageSets($scope.selectedImageSet.name, $scope.imageVariantName);
                $scope.imageVariantName = null;
            });
        };

        $scope.removeImageVariant = function (variant) {
            if (variant.readOnly) {
                displayError("Cannot remove " + variant.name + " because it is required");
                return;
            }
            $http.post(endpoint + "/remove", variant).success(function () {
                loadImageSets($scope.selectedImageSet.name);
                $scope.showBeanrewriterMessage = true;
            });
        };

        $scope.addTranslation = function () {
            if ($scope.selectedImageModel) {
                if (!$scope.selectedImageModel.translations) {
                    $scope.selectedImageModel.translations = [];
                }
                $scope.selectedImageModel.translations.push({"language": "", "message": ""});
            }
        };

        $scope.removeTranslation = function (translation) {
            var idx = $scope.selectedImageModel.translations.indexOf(translation);
            $scope.selectedImageModel.translations.splice(idx, 1);
        };

        $scope.save = function () {
            // save only saves translations, advanced settings and height/width:
            $http.post(endpoint + "/update", $scope.selectedImageModel).success(function () {
                loadImageSets($scope.selectedImageSet.name);
                $scope.selectedImageModel = null;
                $scope.showBeanrewriterMessage = true;
            });
        };

        $scope.init = function () {
            loadImageSets();
        };

        function loadImageSets(selectedImageSetName, selectedImageVariantName) {
            $http.get(endpoint).success(function (data) {
                $scope.imageSets = data;
                if (selectedImageSetName) {
                    $scope.selectedImageSet = null;
                    angular.forEach($scope.imageSets, function(imageSet) {
                        if (imageSet.name === selectedImageSetName) {
                            $scope.selectedImageSet = imageSet;

                            if (selectedImageVariantName) {
                                $scope.selectedImageModel = null;
                                angular.forEach(imageSet.models, function(variant) {
                                    if (variant.name === selectedImageVariantName) {
                                        $scope.selectedImageModel = variant;
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }

        //############################################
        // INIT APP
        //############################################


        $http.get($rootScope.REST.projectSettings).success(function (data) {
            $rootScope.projectSettings = Essentials.keyValueAsDict(data.items);
            $scope.imageSetPrefix = $rootScope.projectSettings ? $rootScope.projectSettings.namespace : '';
        });
        $scope.init();

        //############################################
        // UTIL
        //############################################

        function displayError(msg) {
            $rootScope.feedbackMessages.push({type: 'error', message: msg});
        }

        //############################################
        // DEFAULTS
        //############################################
        $scope.compressionValues = [
            {value: 1, description: "default (uncompressed)", selected: true},
            {value: 0.95, description: "best"},
            {value: 0.9, description: "very good"},
            {value: 0.8, description: "good"},
            {value: 0.7, description: "medium"},
            {value: 0.5, description: "low"}
        ];

        $scope.optimizeValues = [
            {value: "quality", description: "default (quality)", selected: true},
            {value: "speed", description: "speed"},
            {value: "speed.and.quality", description: "speed and quality"},
            {value: "best.quality", description: "best quality"},
            {value: "auto", description: "auto"}
        ];

        $scope.upscalingValues = [
            {value: false, description: "default (off)", selected: true},
            {value: true, description: "on"}
        ];
        $scope.help =
        {imageSet: "Hippo CMS stores several variants of each uploaded image." +
            " All variants of an image are together called an image set, and can be seen in the image gallery in the CMS." +
            " Read more about imagesets here: <a target='_blank' href='http://www.onehippo.org/library/concepts/images-and-assets/create-a-custom-image-set.html'>" +
            "http://www.onehippo.org/library/concepts/images-and-assets/create-a-custom-image-set.html</a>"}
    })
}());
