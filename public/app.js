// --- File Processing Functions (unchanged) ---

async function processFile(file) {
    const reader = new FileReader();
    
    return new Promise((resolve) => {
        reader.onload = async () => {
            let text = '';
            const ext = file.name.split('.').pop().toLowerCase();

            switch(ext) {
                case 'pdf':
                    text = await processPDF(reader.result);
                    break;
                case 'docx':
                    text = await processDOCX(reader.result);
                    break;
                case 'png':
                case 'jpg':
                case 'jpeg':
                    text = await processImage(file);
                    break;
                case 'mp3':
                case 'wav':
                    text = await processAudio(file);
                    break;
                default:
                    text = reader.result;
            }
            
            resolve(text);
        };
        reader.readAsArrayBuffer(file);
    });
}

// PDF processing
async function processPDF(buffer) {
    const pdf = await pdfjsLib.getDocument({data: buffer}).promise;
    let text = '';
    for(let i = 1; i <= pdf.numPages; i++) {
        const page = await pdf.getPage(i);
        const content = await page.getTextContent();
        text += content.items.map(item => item.str).join(' ');
    }
    return text;
}

// Image OCR
async function processImage(file) {
    const worker = await Tesseract.createWorker();
    await worker.loadLanguage('eng');
    await worker.initialize('eng');
    const { data: { text } } = await worker.recognize(file);
    await worker.terminate();
    return text;
}

// --- API Call Function (unchanged) ---
async function callOpenAI(prompt) {
    const response = await fetch('/api/openai', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ prompt })
    });
    
    if(!response.ok) {
        throw new Error(`API Error: ${response.status}`);
    }
    
    return await response.json();
}

// --- Radial Layout Calculation ---
// This function mimics the Python recursive layout in utils.py
function calculateRadialLayout(nodes, edges) {
    // Create a mapping of node id to node
    const nodeMap = {};
    nodes.forEach(node => nodeMap[node.id] = node);

    // Build an adjacency list: parent -> [child, ...]
    const children = {};
    edges.forEach(edge => {
        if (!children[edge.from]) {
            children[edge.from] = [];
        }
        children[edge.from].push(edge.to);
    });
    
    const pos = {};
    function assignPositions(nodeId, angleStart, angleEnd, level) {
        const angle = (angleStart + angleEnd) / 2;
        const radius = level * 100;  // Adjust scale as needed (100px per level)
        pos[nodeId] = [radius * Math.cos(angle), radius * Math.sin(angle)];
        
        if (children[nodeId]) {
            const nChildren = children[nodeId].length;
            const angleStep = (angleEnd - angleStart) / nChildren;
            for (let i = 0; i < nChildren; i++) {
                assignPositions(children[nodeId][i], angleStart + i * angleStep, angleStart + (i + 1) * angleStep, level + 1);
            }
        }
    }
    // Assume the root node has id 'root'
    assignPositions("root", 0, 2 * Math.PI, 1);
    return pos;
}

// --- Mind Map Creation ---
// This creates Plotly traces similar to the Python version (see utils.py)
function createMindMap(graphData) {
    const nodes = graphData.nodes;
    const edges = graphData.edges;
    
    const pos = calculateRadialLayout(nodes, edges);
    
    // Build edge traces for Plotly
    const edgeTraces = edges.map(edge => ({
        x: [pos[edge.from][0], pos[edge.to][0]],
        y: [pos[edge.from][1], pos[edge.to][1]],
        mode: 'lines',
        line: { color: '#888', width: 1 }
    }));
    
    // Build node trace
    const nodeTrace = {
        x: nodes.map(node => pos[node.id][0]),
        y: nodes.map(node => pos[node.id][1]),
        mode: 'markers+text',
        text: nodes.map(node => node.label),
        marker: { size: 12, color: '#ff7f0e' },
        textposition: 'top center'
    };
    
    Plotly.newPlot('mindmap', [...edgeTraces, nodeTrace], {
        showlegend: false,
        margin: { t: 0, b: 0 }
    });
}

// --- Event Listener for Generating the Mind Map ---
// This builds a prompt that mirrors your Python prompt (see app.py) and calls the API.
document.getElementById("generateBtn").addEventListener("click", async function () {
    const textInput = document.getElementById("textInput").value;
    
    if (!textInput) {
        console.error("No text provided");
        return;
    }
    
    const prompt = `Given the following text, create a mind map structure. Extract key concepts and their relationships.
Format the response as a JSON with two arrays:
1. "nodes": Each node has "id" (unique string) and "label" (displayed text)
2. "edges": Each edge has "from" and "to" node IDs showing relationships.
The root node should have id "root". Example format:
{
    "nodes": [{"id": "root", "label": "Main Topic"}, {"id": "1", "label": "Subtopic"}],
    "edges": [{"from": "root", "to": "1"}]
}

Text to analyze:
${textInput}`;
    
    try {
        // Call your OpenAI API via the backend
        const response = await callOpenAI(prompt);
        
        // The API is expected to return a JSON string with nodes and edges
        const graphData = JSON.parse(response.trim());
        
        // Generate the mind map
        createMindMap(graphData);
    } catch (error) {
        console.error("Error generating mind map:", error);
    }
});
