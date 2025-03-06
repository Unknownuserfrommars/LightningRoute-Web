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

function createMindMap(graphData) {
    const nodes = graphData.nodes;
    const edges = graphData.edges;
    
    // Calculate positions (similar to Python code)
    const pos = calculateRadialLayout(nodes, edges);
    
    // Create Plotly traces
    const edgeTraces = edges.map(edge => ({
        x: [pos[edge.from][0], pos[edge.to][0]],
        y: [pos[edge.from][1], pos[edge.to][1]],
        mode: 'lines',
        line: {color: '#888', width: 1}
    }));
    
    const nodeTrace = {
        x: nodes.map(node => pos[node.id][0]),
        y: nodes.map(node => pos[node.id][1]),
        mode: 'markers+text',
        text: nodes.map(node => node.label),
        marker: {size: 12, color: '#ff7f0e'}
    };
    
    Plotly.newPlot('mindmap', [...edgeTraces, nodeTrace], {
        showlegend: false,
        margin: {t: 0, b: 0}
    });
}

async function createDirectoryStructure(graphData) {
    const dirHandle = await window.showDirectoryPicker();
    const root = graphData.nodes.find(n => n.id === 'root');
    
    async function createNodes(parentHandle, nodeId) {
        const children = graphData.edges
            .filter(e => e.from === nodeId)
            .map(e => graphData.nodes.find(n => n.id === e.to));
        
        for(const child of children) {
            const childHandle = await parentHandle.getDirectoryHandle(child.label, {create: true});
            await createNodes(childHandle, child.id);
        }
    }
    
    const rootHandle = await dirHandle.getDirectoryHandle(root.label, {create: true});
    await createNodes(rootHandle, 'root');
}
