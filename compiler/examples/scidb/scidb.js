//libraries
const Project = require("../../src/index").Project;
const Canvas = require("../../src/Canvas").Canvas;
const Jump = require("../../src/Jump").Jump;
const Layer = require("../../src/Layer").Layer;
const View = require("../../src/View").View;

// project components
const renderers = require("./renderers");
const transforms = require("./transforms");
const placements = require("./placements");

// construct a project
var p = new Project("scidb", "../../../config.txt");

//================== phenotype =============
var topWidth = 1000, topHeight = 500;
var topCanvas = new Canvas("phenotype", topWidth, topHeight);
p.addCanvas(topCanvas);

var dotsLayer = new Layer(transforms.dotsTransform, true);
topCanvas.addLayer(dotsLayer);
dotsLayer.addRenderingFunc(renderers.dotsRendering);
// axis layer
topCanvas.addAxes(renderers.topAxes);

// ================== region tab ==============
var regionWidth = 1000000, regionHeight = 500;
var regionCanvas = new Canvas("region", regionWidth, regionHeight);
p.addCanvas(regionCanvas);

var regionLayer = new Layer(transforms.regionTransform, false);
regionCanvas.addLayer(regionLayer);
regionLayer.addRenderingFunc(renderers.regionRendering);
regionLayer.addPlacement(placements.regionPlacement);

// gene layer
var geneLayer = new Layer(transforms.geneTransform, false);
regionCanvas.addLayer(geneLayer);
geneLayer.addRenderingFunc(renderers.geneRendering);
geneLayer.addPlacement(placements.genePlacement);

// axis layer
regionCanvas.addAxes(renderers.regionAxes);

// ================= variant tab ==============
var variantWidth = 1000, variantHeight = 500;
var variantCanvas = new Canvas("variant", variantWidth, variantHeight);
p.addCanvas(variantCanvas);

var variantLayer = new Layer(transforms.variantTransform, true);
variantCanvas.addLayer(variantLayer);
variantLayer.addRenderingFunc(renderers.variantRendering);
// axis layer
variantCanvas.addAxes(renderers.variantAxes);

// ================== Views ===================
var view = new View("dotview", 0, 0, 1000, 500);
p.addView(view);
p.setInitialStates(view, topCanvas, 0, 0);

// ==================phenotype -> region ======
var selector = function () {
    return true;
};

var newViewport = function (row) {
    return {"constant": [row.pos/250, 0]};
};

var newPredicate = function (row) {
    var pred = {"AND" : [
               { "==" : ["chrom", row.chrom]},
               { "==" : ["pos", row.pos]},
               ]};
    return {"layer0" : pred};
};

var jumpName = function (row) {
    return row.chrom + ":" + row.pos;
};

p.addJump(new Jump(topCanvas, regionCanvas, "semantic_zoom", {selector : selector, viewport : newViewport, predicates : newPredicate, name : jumpName}));

// ================= region -> variant =========
var selector = function () {
    return true;
};

var newViewport = function (row) {
    return {"constant" : [0 ,0]};
};

var newPredicate = function (row) {
    var pred = {"AND" : [
               { "==" : ["chrom", row.chrom]},
               { "==" : ["pos", row.pos]},
               ]};
    return {"layer0" : pred};
};

var jumpName = function (row) {
    return row.chrom + ":" + row.pos;
};

p.addJump(new Jump(regionCanvas, variantCanvas, "semantic_zoom", {selector : selector, viewport : newViewport, predicates : newPredicate, name : jumpName}));

// ================ variant -> phenotype =========
var selector = function () {
    return true;
};

var newViewport = function (row) {
    return {"constant" : [0 ,0]};
};

var newPredicate = function (row) {
    var pred =  { "==" : ["id", row.sub_field_id]};
    return {"layer0" : pred};
};

var jumpName = function (row) {
    return row.title + ":" + row.description;
};

p.addJump(new Jump(variantCanvas,topCanvas, "semantic_zoom", {selector : selector, viewport : newViewport, predicates : newPredicate, name : jumpName}));

// save to db
p.saveProject();

