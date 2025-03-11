// DOM Elements
const textTab = document.getElementById('text-tab');
const fileTab = document.getElementById('file-tab');
const textInput = document.getElementById('text-input');
const fileInput = document.getElementById('file-input');
const inputText = document.getElementById('input-text');
const generateBtn = document.getElementById('generate-btn');
const uploadBtn = document.getElementById('upload-btn');
const fileInputField = document.getElementById('file-input-field');
const dropArea = document.getElementById('drop-area');
const fileInfo = document.getElementById('file-info');
const fileName = document.getElementById('file-name');
const modelSelect = document.getElementById('model-select');
const loading = document.getElementById('loading');
const mindMapContainer = document.getElementById('mind-map-container');
const mindMapElement = document.getElementById('mind-map');
const zoomIn = document.getElementById('zoom-in');
const zoomOut = document.getElementById('zoom-out');
const resetView = document.getElementById('reset-view');
const downloadImg = document.getElementById('download-img');
const errorModal = document.getElementById('error-modal');
const errorMessage = document.getElementById('error-message');
const closeModal = document.querySelector('.close-modal');

// State variables
let mindMap = null;
let zoom = d3.zoom();
let svg, g;
let currentScale = 1;

// Tab switching
textTab.addEventListener('click', () => {
    textTab.classList.add('active');
    fileTab.classList.remove('active');
    textInput.classList.add('active');
    fileInput.classList.remove('active');
});

fileTab.addEventListener('click', () => {
    fileTab.classList.add('active');
    textTab.classList.remove('active');
    fileInput.classList.add('active');
    textInput.classList.remove('active');
});

// File upload handling
dropArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropArea.classList.add('active');
});

dropArea.addEventListener('dragleave', () => {
    dropArea.classList.remove('active');
});

dropArea.addEventListener('drop', (e) => {
    e.preventDefault();
    dropArea.classList.remove('active');
    if (e.dataTransfer.files.length) {
        fileInputField.files = e.dataTransfer.files;
        updateFileInfo();
    }
});

dropArea.addEventListener('click', () => {
    fileInputField.click();
});

fileInputField.addEventListener('change', updateFileInfo);

function updateFileInfo() {
    if (fileInputField.files.length) {
        const file = fileInputField.files[0];
        fileName.textContent = file.name;
        fileInfo.classList.remove('hidden');
    } else {
        fileInfo.classList.add('hidden');
    }
}

// Generate mind map from text
generateBtn.addEventListener('click', async () => {
    const text = inputText.value.trim();
    if (!text) {
        showError('Please enter some text to generate a mind map.');
        return;
    }
    generateMindMap(text);
});

// Generate mind map from file
uploadBtn.addEventListener('click', async () => {
    if (!fileInputField.files.length) {
        showError('Please select a file to upload.');
        return;
    }
    uploadFile();
});

// Mind map generation from text
async function generateMindMap(text) {
    showLoading(true);
    
    try {
        const response = await fetch('/api/mindmap/generate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                text: text,
                model: modelSelect.value
            })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error ${response.status}`);
        }
        
        const data = await response.json();
        renderMindMap(data);
    } catch (error) {
        console.error('Error generating mind map:', error);
        showError('Failed to generate mind map. Please try again later.');
        showLoading(false);
    }
}

// File upload and mind map generation
async function uploadFile() {
    showLoading(true);
    
    const formData = new FormData();
    formData.append('file', fileInputField.files[0]);
    
    try {
        const response = await fetch('/api/mindmap/upload', {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error ${response.status}`);
        }
        
        const data = await response.json();
        renderMindMap(data);
    } catch (error) {
        console.error('Error uploading file:', error);
        showError('Failed to process file. Please try a different file format or try again later.');
        showLoading(false);
    }
}

// Render the mind map visualization
function renderMindMap(data) {
    mindMap = data;
    showLoading(false);
    mindMapContainer.classList.remove('hidden');
    
    // Clear previous mind map
    mindMapElement.innerHTML = '';
    
    // Calculate dimensions
    const width = mindMapElement.clientWidth;
    const height = mindMapElement.clientHeight;
    
    // Create SVG
    svg = d3.select('#mind-map')
        .append('svg')
        .attr('width', '100%')
        .attr('height', '100%')
        .attr('viewBox', [0, 0, width, height])
        .call(zoom.on('zoom', (event) => {
            currentScale = event.transform.k;
            g.attr('transform', event.transform);
        }));
    
    // Create container group for zooming
    g = svg.append('g');
    
    // Create tooltip
    const tooltip = d3.select('#mind-map')
        .append('div')
        .attr('class', 'tooltip')
        .style('opacity', 0);
    
    // Create hierarchical data
    const root = createHierarchy(data);
    
    // Create tree layout
    const treeLayout = d3.tree()
        .size([height - 100, width - 200])
        .nodeSize([80, 200]);
    
    const rootNode = d3.hierarchy(root);
    treeLayout(rootNode);
    
    // Swap x and y coordinates for horizontal layout
    rootNode.descendants().forEach(d => {
        const temp = d.x;
        d.x = d.y + 100; // Add padding
        d.y = temp + height/2 - 100; // Center vertically
    });
    
    // Create links
    g.selectAll('.link')
        .data(rootNode.links())
        .enter()
        .append('path')
        .attr('class', 'link')
        .attr('d', d3.linkHorizontal()
            .x(d => d.x)
            .y(d => d.y)
        );
    
    // Create node groups
    const nodes = g.selectAll('.node')
        .data(rootNode.descendants())
        .enter()
        .append('g')
        .attr('class', d => `node ${d.data.category}`)
        .attr('transform', d => `translate(${d.x},${d.y})`);
    
    // Add circles to nodes
    nodes.append('circle')
        .attr('r', d => d.data.category === 'root' ? 30 : 20)
        .on('mouseover', function(event, d) {
            if (d.data.tooltip) {
                tooltip.transition()
                    .duration(200)
                    .style('opacity', 0.9);
                tooltip.html(d.data.tooltip)
                    .style('left', (event.pageX - mindMapElement.getBoundingClientRect().left + 10) + 'px')
                    .style('top', (event.pageY - mindMapElement.getBoundingClientRect().top - 20) + 'px');
            }
        })
        .on('mouseout', function() {
            tooltip.transition()
                .duration(500)
                .style('opacity', 0);
        });
    
    // Add text to nodes
    nodes.append('text')
        .attr('dy', d => d.data.category === 'root' ? 0 : '0.35em')
        .text(d => {
            let label = d.data.label;
            // Truncate long labels
            if (label.length > 15) {
                label = label.substring(0, 12) + '...';
            }
            return label;
        });
    
    // Center the graph
    const rootTransform = rootNode.descendants()[0];
    const initialTransform = d3.zoomIdentity.translate(
        width / 2 - rootTransform.x,
        height / 2 - rootTransform.y
    );
    
    svg.call(zoom.transform, initialTransform);
}

// Convert mind map data to hierarchy for D3
function createHierarchy(mindMap) {
    // Find root node
    const rootNode = mindMap.nodes.find(node => node.id === mindMap.rootNodeId);
    if (!rootNode) {
        showError('Invalid mind map data: Root node not found');
        return null;
    }
    
    // Create hierarchical structure
    const hierarchyNode = {
        id: rootNode.id,
        label: rootNode.label,
        category: rootNode.category || 'root',
        tooltip: rootNode.tooltip || '',
        children: []
    };
    
    // Process nodes recursively
    addChildrenToNode(hierarchyNode, rootNode.id, mindMap.nodes, []);
    
    return hierarchyNode;
}

// Recursively add children to a node
function addChildrenToNode(hierarchyNode, nodeId, allNodes, visited) {
    // Avoid circular references
    if (visited.includes(nodeId)) {
        return;
    }
    
    visited.push(nodeId);
    
    // Find the node in the full nodes list
    const node = allNodes.find(n => n.id === nodeId);
    if (!node || !node.connections) {
        return;
    }
    
    // Add each connected node as a child
    for (const connection of node.connections) {
        const childId = connection.target;
        const childNode = allNodes.find(n => n.id === childId);
        
        if (childNode && !visited.includes(childId)) {
            const childHierarchyNode = {
                id: childNode.id,
                label: childNode.label,
                category: childNode.category || 'concept',
                tooltip: `${connection.relationship || 'Related to'}: ${childNode.tooltip || childNode.label}`,
                children: []
            };
            
            hierarchyNode.children.push(childHierarchyNode);
            addChildrenToNode(childHierarchyNode, childId, allNodes, [...visited]);
        }
    }
}

// Mind map control handlers
zoomIn.addEventListener('click', () => {
    svg.call(zoom.scaleBy, 1.2);
});

zoomOut.addEventListener('click', () => {
    svg.call(zoom.scaleBy, 0.8);
});

resetView.addEventListener('click', () => {
    const width = mindMapElement.clientWidth;
    const height = mindMapElement.clientHeight;
    
    svg.transition()
        .duration(750)
        .call(
            zoom.transform,
            d3.zoomIdentity.translate(width / 2, height / 2).scale(1)
        );
});

downloadImg.addEventListener('click', () => {
    // Create a copy of the SVG for export
    const svgCopy = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svgCopy.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
    svgCopy.innerHTML = svg.node().innerHTML;
    
    // Add CSS styles inline
    const styleElement = document.createElement('style');
    styleElement.textContent = `
        .node circle { fill: white; stroke-width: 2px; }
        .node.root circle { fill: #3498db; stroke: #2980b9; }
        .node.concept circle { stroke: #2ecc71; }
        .node.example circle { stroke: #f39c12; }
        .node.definition circle { stroke: #3498db; }
        .node text { font-size: 12px; text-anchor: middle; dominant-baseline: middle; fill: black; }
        .node.root text { fill: white; font-weight: bold; }
        .link { fill: none; stroke: #ccc; stroke-width: 1.5px; }
    `;
    svgCopy.prepend(styleElement);
    
    // Convert SVG to data URL
    const svgData = new XMLSerializer().serializeToString(svgCopy);
    const svgBlob = new Blob([svgData], {type: 'image/svg+xml;charset=utf-8'});
    const svgUrl = URL.createObjectURL(svgBlob);
    
    // Create download link
    const downloadLink = document.createElement('a');
    downloadLink.href = svgUrl;
    downloadLink.download = 'mind_map.svg';
    document.body.appendChild(downloadLink);
    downloadLink.click();
    document.body.removeChild(downloadLink);
});

// Utility functions
function showLoading(isLoading) {
    if (isLoading) {
        loading.classList.remove('hidden');
        mindMapContainer.classList.add('hidden');
    } else {
        loading.classList.add('hidden');
    }
}

function showError(message) {
    errorMessage.textContent = message;
    errorModal.classList.remove('hidden');
}

closeModal.addEventListener('click', () => {
    errorModal.classList.add('hidden');
});

// Handle window resize
window.addEventListener('resize', () => {
    if (mindMap) {
        renderMindMap(mindMap);
    }
});

// Make an initial health check
async function checkHealth() {
    try {
        const response = await fetch('/api/mindmap/health');
        if (response.ok) {
            console.log('Mind Map API is ready!');
        } else {
            console.error('API health check failed');
        }
    } catch (error) {
        console.error('Unable to connect to the API');
    }
}

// Initialize
document.addEventListener('DOMContentLoaded', checkHealth);
